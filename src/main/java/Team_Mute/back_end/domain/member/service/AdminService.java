package Team_Mute.back_end.domain.member.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Team_Mute.back_end.domain.member.dto.request.AdminAccountDeleteRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminAccountUpdateRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminPasswordResetRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminPasswordUpdateRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminRoleUpdateRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminSignupRequestDto;
import Team_Mute.back_end.domain.member.dto.request.PasswordResetRequestDto;
import Team_Mute.back_end.domain.member.dto.request.PasswordUpdateRequestDto;
import Team_Mute.back_end.domain.member.dto.response.AdminInfoResponse;
import Team_Mute.back_end.domain.member.dto.response.AdminSignupResponseDto;
import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.entity.AdminRegion;
import Team_Mute.back_end.domain.member.entity.UserCompany;
import Team_Mute.back_end.domain.member.entity.UserRole;
import Team_Mute.back_end.domain.member.exception.DuplicateEmailException;
import Team_Mute.back_end.domain.member.exception.InvalidPasswordException;
import Team_Mute.back_end.domain.member.exception.UserNotFoundException;
import Team_Mute.back_end.domain.member.exception.UserRegistrationException;
import Team_Mute.back_end.domain.member.repository.AdminRegionRepository;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.member.repository.UserCompanyRepository;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.member.repository.UserRoleRepository;
import Team_Mute.back_end.domain.member.session.SessionStore;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 비즈니스 로직 서비스 클래스
 * 관리자 계정의 생명주기 전체를 관리하는 핵심 서비스
 *
 * 주요 기능:
 * - 관리자 회원가입 및 초기 마스터 관리자 생성
 * - 관리자 정보 조회, 수정, 삭제 (CRUD)
 * - 비밀번호 변경 및 초기화 (임시 비밀번호 발송)
 * - 권한 관리 (마스터 관리자만 수행 가능)
 * - 담당 지역 및 소속 기업 관리
 * - Redis 세션 및 Token Version 관리
 *
 * 보안 기능:
 * - 마스터 관리자 권한 검증
 * - Token Version 증가로 기존 JWT 토큰 무효화
 * - Redis 세션 삭제로 즉시 로그아웃 처리
 * - BCrypt 암호화로 비밀번호 보호
 *
 * @author Team Mute
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

	/**
	 * 마스터 관리자 정보
	 * - 초기 마스터 관리자 계정 생성에 사용
	 */
	@Value("${master.account.email}")
	private String masterEmail;

	@Value("${master.account.name}")
	private String masterName;

	@Value("${master.account.password}")
	private String masterPassword;

	@Value("${master.account.phone}")
	private String masterPhone;

	private final UserRepository userRepository;
	private final AdminRepository adminRepository;
	private final UserCompanyRepository userCompanyRepository;
	private final PasswordService passwordService;
	private final SessionStore sessionStore;
	private final EmailService emailService;
	private final AdminRegionRepository adminRegionRepository;
	private final UserRoleRepository userRoleRepository;

	/**
	 * 기업 정보 조회 또는 생성
	 * - 기업명으로 기존 기업 조회
	 * - 존재하지 않으면 새로운 기업 생성
	 * - 회원가입 시 소속 기업 설정에 사용
	 * - 중복 기업 생성 방지
	 *
	 * @param companyName 조회 또는 생성할 기업명
	 * @return UserCompany 엔티티 (기존 또는 새로 생성된)
	 * @throws UserRegistrationException 기업 생성 실패 시
	 */
	private UserCompany getOrCreateCompany(String companyName) {
		return userCompanyRepository.findByCompanyName(companyName)
			.orElseGet(() -> createNewCompany(companyName));
	}

	/**
	 * 새로운 기업 생성
	 * - 최대 Company ID 조회 후 +1하여 새 ID 할당
	 * - 기업명과 등록 일시 설정
	 * - 데이터베이스에 저장
	 *
	 * @param companyName 생성할 기업명
	 * @return 저장된 UserCompany 엔티티
	 * @throws UserRegistrationException 기업 생성 중 오류 발생 시
	 */
	private UserCompany createNewCompany(String companyName) {
		try {
			// 최대 Company ID 조회 (데이터가 없으면 0)
			Integer maxCompanyId = userCompanyRepository.findMaxCompanyId().orElse(0);

			// 새로운 기업 엔티티 생성
			UserCompany newCompany = UserCompany.builder()
				.companyId(maxCompanyId + 1)
				.companyName(companyName)
				.regDate(LocalDateTime.now())
				.build();

			UserCompany savedCompany = userCompanyRepository.save(newCompany);
			return savedCompany;
		} catch (Exception e) {
			log.error("Failed to create new company: {}", companyName, e);
			throw new UserRegistrationException("회사 정보 처리 중 오류가 발생했습니다.");
		}
	}

	/**
	 * 관리자 삭제
	 * - Redis에 저장된 모든 세션 삭제 (즉시 로그아웃)
	 * - Refresh Token 블랙리스트 등록
	 * - 데이터베이스에서 관리자 정보 삭제
	 *
	 * 처리 흐름:
	 * 1. Redis에서 해당 사용자의 모든 세션 ID 조회
	 * 2. 각 세션의 Refresh Token JTI 조회
	 * 3. Refresh Token을 블랙리스트에 등록 (7일간 유지)
	 * 4. Redis에서 세션 정보 삭제
	 * 5. 데이터베이스에서 관리자 엔티티 삭제
	 *
	 * @param userId 삭제할 관리자 ID
	 * @throws UserNotFoundException 관리자를 찾을 수 없는 경우
	 */
	public void deleteUser(Long userId) {
		// 1. Redis에서 해당 사용자의 모든 세션 ID 조회
		Set<String> sessionIds = sessionStore.getUserSids(userId.toString());

		// 2. 세션이 존재하는 경우 처리
		if (sessionIds != null && !sessionIds.isEmpty()) {
			for (String sid : sessionIds) {
				// 3. 각 세션의 Refresh Token JTI 조회
				String rtJti = sessionStore.getCurrentRtJti(sid);
				if (rtJti != null) {
					// 4. Refresh Token을 블랙리스트에 등록 (7일간 유지)
					sessionStore.revokeRt(rtJti, java.time.Duration.ofDays(7));
				}
				// 5. Redis에서 세션 삭제
				sessionStore.deleteSession(sid, userId.toString());
			}
			log.info("Redis 세션 삭제 완료: userId={}", userId);
		}

		// 6. 데이터베이스에서 관리자 조회
		Admin user = adminRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		// 7. 데이터베이스에서 관리자 삭제
		adminRepository.delete(user);
		log.info("DB 회원 정보 삭제 완료: userId={}", userId);
	}

	/**
	 * 관리자 비밀번호 변경
	 * - 기존 비밀번호 검증
	 * - 새 비밀번호 강도 검증 (영문, 숫자, 특수문자 포함 8자 이상)
	 * - 새 비밀번호 BCrypt 암호화 저장
	 * - Token Version 증가로 기존 JWT 토큰 무효화
	 *
	 * 보안 고려사항:
	 * - 기존 비밀번호 일치 여부 확인 (본인 확인)
	 * - Token Version 증가로 비밀번호 변경 전 발급된 모든 토큰 무효화
	 * - 비밀번호 변경 후 재로그인 필요
	 *
	 * @param userId 비밀번호를 변경할 관리자 ID
	 * @param requestDto 비밀번호 변경 요청 DTO (기존 비밀번호, 새 비밀번호)
	 * @throws UserNotFoundException 관리자를 찾을 수 없는 경우
	 * @throws InvalidPasswordException 기존 비밀번호가 일치하지 않는 경우
	 * @throws IllegalArgumentException 새 비밀번호가 강도 요구사항을 충족하지 않는 경우
	 */
	public void updatePassword(Long userId, PasswordUpdateRequestDto requestDto) {
		// 1. 관리자 조회
		Admin user = adminRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		// 2. 기존 비밀번호 검증
		if (!passwordService.matches(requestDto.getPassword(), user.getAdminPwd())) {
			throw new InvalidPasswordException();
		}

		// 3. 새 비밀번호 강도 검증
		if (!passwordService.isStrongPassword(requestDto.getNewPassword())) {
			throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.");
		}

		// 4. 새 비밀번호 암호화 및 저장
		user.setAdminPwd(passwordService.encodePassword(requestDto.getNewPassword()));

		// 5. Token Version 증가 (기존 JWT 토큰 무효화)
		user.setTokenVer(user.getTokenVer() + 1);

		log.info("비밀번호 수정 및 토큰 버전 증가 완료: userId={}", userId);
	}

	/**
	 * 이메일 존재 여부 확인
	 * - 읽기 전용 트랜잭션으로 성능 최적화
	 * - 관리자 회원가입 시 이메일 중복 체크에 사용
	 *
	 * @param email 확인할 이메일 주소
	 * @return 존재하면 true, 존재하지 않으면 false
	 */
	@Transactional(readOnly = true)
	public boolean isEmailExists(String email) {
		return adminRepository.existsByAdminEmail(email);
	}

	/**
	 * Token Version 조회
	 * - 읽기 전용 트랜잭션으로 성능 최적화
	 * - JWT Refresh Token 재발급 시 Token Version 검증에 사용
	 * - AdminAuthService의 refresh 메서드에서 호출
	 *
	 * @param userId 조회할 관리자 ID
	 * @return Token Version
	 * @throws IllegalArgumentException 관리자를 찾을 수 없는 경우
	 */
	@Transactional(readOnly = true)
	public Integer getTokenVer(Long userId) {
		return adminRepository.findTokenVerByAdminId(userId)
			.orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
	}

	/**
	 * 관리자 비밀번호 초기화
	 * - 임시 비밀번호 생성 (10자리 무작위 문자열)
	 * - 이메일로 임시 비밀번호 발송
	 * - 임시 비밀번호를 BCrypt 암호화하여 저장
	 * - Token Version 증가로 기존 JWT 토큰 무효화
	 *
	 * 처리 흐름:
	 * 1. 이메일로 관리자 조회
	 * 2. 10자리 무작위 임시 비밀번호 생성
	 * 3. 임시 비밀번호를 이메일로 발송
	 * 4. 임시 비밀번호를 암호화하여 데이터베이스에 저장
	 * 5. Token Version 증가
	 *
	 * @param requestDto 비밀번호 초기화 요청 DTO (이메일 주소)
	 * @throws UserNotFoundException 관리자를 찾을 수 없는 경우
	 */
	public void resetPassword(PasswordResetRequestDto requestDto) {
		// 1. 이메일로 관리자 조회
		Admin user = adminRepository.findByAdminEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		// 2. 10자리 무작위 임시 비밀번호 생성
		String temporaryPassword = generateRandomPassword();

		// 3. 임시 비밀번호를 이메일로 발송
		emailService.sendTemporaryPassword(user.getAdminEmail(), temporaryPassword);

		// 4. 임시 비밀번호를 암호화하여 저장
		user.setAdminPwd(passwordService.encodePassword(temporaryPassword));

		// 5. Token Version 증가 (기존 JWT 토큰 무효화)
		user.setTokenVer(user.getTokenVer() + 1);

		log.info("비밀번호 초기화 및 DB 업데이트 완료: userId={}", user.getAdminId());
	}

	/**
	 * 관리자 정보 조회
	 * - 읽기 전용 트랜잭션으로 성능 최적화
	 * - 관리자 ID로 상세 정보 조회
	 * - 담당 지역 정보 포함 (마스터 관리자는 "Master" 표시)
	 * - AdminController의 getAdminInfo API에서 호출
	 *
	 * 처리 로직:
	 * - adminRegion이 null인 경우: "Master" 표시 (마스터 관리자)
	 * - adminRegion이 존재하는 경우: 지역명 조회
	 * - 지역 정보가 없는 경우: "지역 정보 없음" 표시
	 *
	 * @param adminId 조회할 관리자 ID
	 * @return AdminInfoResponse DTO (역할, 지역, 이메일, 이름, 전화번호)
	 * @throws UserNotFoundException 관리자를 찾을 수 없는 경우
	 */
	@Transactional(readOnly = true)
	public AdminInfoResponse getAdminInfo(Long adminId) {
		// 1. 관리자 조회
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		// 2. 담당 지역 정보 처리
		String regionName = "N/A";
		if (admin.getAdminRegion() != null && admin.getAdminRegion().getRegionId() != null) {
			// 지역 ID로 지역명 조회
			regionName = adminRegionRepository.findById(admin.getAdminRegion().getRegionId())
				.map(AdminRegion::getRegionName)
				.orElse("지역 정보 없음");
		} else {
			// 마스터 관리자는 담당 지역 없음
			regionName = "Master";
		}

		// 3. AdminInfoResponse 생성 및 반환
		return new AdminInfoResponse(
			admin.getUserRole().getRoleId(),
			regionName,
			admin.getAdminEmail(),
			admin.getAdminName(),
			admin.getAdminPhone()
		);
	}

	/**
	 * 관리자 정보 수정
	 * - 이메일, 이름, 전화번호, 담당 지역 수정 가능
	 * - 이메일 변경 시 중복 검증
	 * - 지역 정보가 없으면 새로 생성
	 *
	 * 처리 흐름:
	 * 1. 관리자 조회
	 * 2. 이메일 변경 시 중복 체크
	 * 3. 담당 지역 설정 (기존 지역 또는 새로 생성)
	 * 4. 이름 및 전화번호 수정
	 *
	 * @param adminId 수정할 관리자 ID
	 * @param requestDto 수정할 정보 DTO
	 * @throws UserNotFoundException 관리자를 찾을 수 없는 경우
	 * @throws DuplicateEmailException 변경하려는 이메일이 이미 사용 중인 경우
	 */
	public void updateAdminInfo(Long adminId, AdminAccountUpdateRequest requestDto) {
		// 1. 관리자 조회
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		// 2. 이메일 변경 시 중복 체크
		if (!admin.getAdminEmail().equals(requestDto.getUserEmail())) {
			if (adminRepository.existsByAdminEmail(requestDto.getUserEmail())) {
				throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
			}
			admin.setAdminEmail(requestDto.getUserEmail());
		}

		// 3. 담당 지역 설정
		if (requestDto.getRegionName() != null) {
			AdminRegion region = findOrCreateRegion(requestDto.getRegionName());
			admin.setAdminRegion(region);
		}

		// 4. 이름 및 전화번호 수정
		admin.setAdminName(requestDto.getUserName());
		admin.setAdminPhone(requestDto.getUserPhone());

		log.info("관리자 정보 수정 완료: adminId={}", adminId);
	}

	/**
	 * 관리자 계정 삭제 (마스터 관리자 전용)
	 * - 마스터 관리자 권한 검증
	 * - 자기 자신 삭제 방지
	 * - 다른 마스터 관리자 삭제 방지
	 * - 대상 관리자의 모든 세션 삭제 및 계정 삭제
	 *
	 * 보안 검증:
	 * 1. 호출자가 마스터 관리자인지 확인 (roleId=0)
	 * 2. 자기 자신을 삭제하려는지 확인
	 * 3. 삭제 대상이 마스터 관리자인지 확인
	 *
	 * @param authentication 인증 정보 (호출자의 관리자 ID 포함)
	 * @param requestDto 삭제할 관리자의 이메일
	 * @throws UserNotFoundException 관리자를 찾을 수 없는 경우
	 * @throws AccessDeniedException 호출자가 마스터 관리자가 아니거나 삭제 대상이 마스터 관리자인 경우
	 * @throws IllegalArgumentException 자기 자신을 삭제하려는 경우
	 */
	public void deleteAdminAccount(Authentication authentication, AdminAccountDeleteRequest requestDto) {
		// 1. 마스터 관리자 권한 검증
		Admin caller = checkMasterAuthority(authentication);

		// 2. 삭제 대상 관리자 조회
		Admin targetUser = adminRepository.findByAdminEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		// 3. 자기 자신 삭제 방지
		if (caller.getAdminId().equals(targetUser.getAdminId())) {
			throw new IllegalArgumentException("자신을 삭제할 수 없습니다.");
		}

		// 4. 마스터 관리자 삭제 방지
		if (targetUser.getUserRole().getRoleId() == 0) {
			throw new AccessDeniedException("Master 관리자는 삭제할 수 없습니다.");
		}

		// 5. 관리자 삭제 (세션 및 데이터베이스)
		deleteUser(targetUser.getAdminId());
	}

	/**
	 * 관리자 비밀번호 수정
	 * - 기존 비밀번호 검증
	 * - 새 비밀번호 암호화 저장
	 * - Token Version 증가로 기존 JWT 토큰 무효화
	 *
	 * @param adminId 비밀번호를 변경할 관리자 ID
	 * @param requestDto 비밀번호 변경 요청 DTO
	 * @throws UserNotFoundException 관리자를 찾을 수 없는 경우
	 * @throws InvalidPasswordException 기존 비밀번호가 일치하지 않는 경우
	 */
	public void updateAdminPassword(Long adminId, AdminPasswordUpdateRequest requestDto) {
		// 1. 관리자 조회
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		// 2. 기존 비밀번호 검증
		if (!passwordService.matches(requestDto.getPassword(), admin.getAdminPwd())) {
			throw new InvalidPasswordException();
		}

		// 3. 새 비밀번호 암호화 및 저장
		admin.setAdminPwd(passwordService.encodePassword(requestDto.getNewPassword()));

		// 4. Token Version 증가
		admin.setTokenVer(admin.getTokenVer() + 1);

		log.info("관리자 비밀번호 수정 완료: adminId={}", adminId);
	}

	/**
	 * 관리자 비밀번호 초기화
	 * - 관리자 계정(roleId ≤ 2)만 초기화 가능
	 * - 임시 비밀번호 생성 및 이메일 발송
	 * - Token Version 증가로 기존 JWT 토큰 무효화
	 *
	 * @param requestDto 비밀번호 초기화 요청 DTO
	 * @throws UserNotFoundException 관리자를 찾을 수 없는 경우
	 * @throws AccessDeniedException 일반 사용자 계정인 경우
	 */
	public void resetAdminPassword(AdminPasswordResetRequest requestDto) {
		// 1. 이메일로 관리자 조회
		Admin user = adminRepository.findByAdminEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		// 2. 관리자 계정인지 확인 (roleId ≤ 2)
		if (user.getUserRole().getRoleId() > 2) {
			throw new AccessDeniedException("관리자 계정만 초기화할 수 있습니다.");
		}

		// 3. 임시 비밀번호 생성
		String temporaryPassword = generateRandomPassword();

		// 4. 임시 비밀번호 이메일 발송
		emailService.sendTemporaryPassword(user.getAdminEmail(), temporaryPassword);

		// 5. 임시 비밀번호 암호화 저장
		user.setAdminPwd(passwordService.encodePassword(temporaryPassword));

		// 6. Token Version 증가
		user.setTokenVer(user.getTokenVer() + 1);

		log.info("관리자 비밀번호 초기화 및 DB 업데이트 완료: userId={}", user.getAdminId());
	}

	/**
	 * 관리자 권한 수정 (마스터 관리자 전용)
	 * - 마스터 관리자만 다른 관리자의 권한 수정 가능
	 * - 자기 자신의 권한 수정 방지
	 * - Token Version 증가로 기존 JWT 토큰 무효화 (권한 정보 갱신)
	 *
	 * 권한 수정 가능 범위:
	 * - roleId=1: 2차 승인자
	 * - roleId=2: 1차 승인자
	 * - roleId=0(마스터 관리자)로는 변경 불가
	 *
	 * @param authentication 인증 정보 (호출자의 관리자 ID 포함)
	 * @param requestDto 권한 수정 요청 DTO (대상 이메일, 새 역할 ID)
	 * @throws UserNotFoundException 관리자를 찾을 수 없는 경우
	 * @throws AccessDeniedException 호출자가 마스터 관리자가 아닌 경우
	 * @throws IllegalArgumentException 자기 자신의 권한을 수정하려는 경우
	 * @throws EntityNotFoundException 역할 정보를 찾을 수 없는 경우
	 */
	public void updateAdminRole(Authentication authentication, AdminRoleUpdateRequest requestDto) {
		// 1. 마스터 관리자 권한 검증
		Admin caller = checkMasterAuthority(authentication);

		// 2. 권한을 수정할 대상 관리자 조회
		Admin targetUser = adminRepository.findByAdminEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		// 3. 자기 자신의 권한 수정 방지
		if (caller.getAdminId().equals(targetUser.getAdminId())) {
			throw new IllegalArgumentException("자신의 권한을 수정할 수 없습니다.");
		}

		// 4. 새 역할 조회
		UserRole roleToSet = userRoleRepository.findById(requestDto.getRoleId())
			.orElseThrow(() -> new EntityNotFoundException("해당 역할을 찾을 수 없습니다."));

		// 5. 역할 변경
		targetUser.setUserRole(roleToSet);

		// 6. Token Version 증가 (JWT 토큰의 권한 정보 갱신)
		targetUser.setTokenVer(targetUser.getTokenVer() + 1);

		log.info("관리자 권한 수정 완료: targetUserId={}, newRoleId={}", targetUser.getAdminId(), requestDto.getRoleId());
	}

	/**
	 * 마스터 관리자 권한 검증
	 * - 호출자가 마스터 관리자(roleId=0)인지 확인
	 * - 마스터 관리자만 수행 가능한 작업에 사용
	 * - 권한이 없는 경우 AccessDeniedException 발생
	 *
	 * 사용처:
	 * - 관리자 계정 삭제
	 * - 관리자 권한 수정
	 *
	 * @param authentication 인증 정보 (호출자의 관리자 ID 포함)
	 * @return 호출자의 Admin 엔티티
	 * @throws UserNotFoundException 호출자를 찾을 수 없는 경우
	 * @throws AccessDeniedException 호출자가 마스터 관리자가 아닌 경우
	 */
	private Admin checkMasterAuthority(Authentication authentication) {
		// 1. 인증 정보에서 관리자 ID 추출
		Long callerId = Long.valueOf((String)authentication.getPrincipal());

		// 2. 호출자 조회
		Admin caller = adminRepository.findById(callerId)
			.orElseThrow(UserNotFoundException::new);

		// 3. 마스터 관리자 권한 확인
		if (caller.getUserRole().getRoleId() != 0) {
			throw new AccessDeniedException("Master 관리자만 이 작업을 수행할 수 있습니다.");
		}

		return caller;
	}

	/**
	 * 관리자 회원가입
	 * - 마스터 관리자가 새로운 관리자 계정 생성
	 * - roleId 1(2차 승인자) 또는 2(1차 승인자)만 생성 가능
	 * - 임시 비밀번호 생성 및 이메일 발송
	 * - 담당 지역 및 소속 기업 설정
	 *
	 * 처리 흐름:
	 * 1. 역할 ID 검증 (1 또는 2만 허용)
	 * 2. 이메일 중복 확인
	 * 3. 임시 비밀번호 생성
	 * 4. 담당 지역 조회 또는 생성
	 * 5. 소속 기업 조회 또는 생성 (기본: "신한금융희망재단")
	 * 6. 관리자 엔티티 생성 및 저장
	 * 7. 환영 이메일 발송 (임시 비밀번호 포함)
	 *
	 * @param requestDto 관리자 회원가입 요청 DTO
	 * @return AdminSignupResponseDto (생성된 관리자 정보)
	 * @throws IllegalArgumentException 역할 ID가 1 또는 2가 아닌 경우
	 * @throws DuplicateEmailException 이메일이 이미 존재하는 경우
	 */
	public AdminSignupResponseDto adminSignUp(AdminSignupRequestDto requestDto) {
		// 1. 역할 ID 검증 (1 또는 2만 허용)
		if (requestDto.getRoleId() != 1 && requestDto.getRoleId() != 2) {
			throw new IllegalArgumentException("관리자 계정 생성은 역할 ID 1 또는 2만 가능합니다.");
		}

		// 2. 이메일 중복 확인
		if (adminRepository.existsByAdminEmail(requestDto.getUserEmail())) {
			throw new DuplicateEmailException("이미 가입된 이메일 입니다.");
		}

		// 3. 임시 비밀번호 생성 (10자리)
		String temporaryPassword = generateRandomPassword();

		// 4. 담당 지역 조회 또는 생성
		AdminRegion region = findOrCreateRegion(requestDto.getRegionName());

		// 5. 임시 비밀번호 암호화
		String encodedPassword = passwordService.encodePassword(temporaryPassword);

		// 6. 소속 기업 조회 또는 생성 (기본: "신한금융희망재단")
		UserCompany userCompany = getOrCreateCompany("신한금융희망재단");

		// 7. 역할 조회
		UserRole adminRole = userRoleRepository.findById(requestDto.getRoleId()).orElseThrow();

		// 8. Admin 엔티티 생성
		Admin user = Admin.builder()
			.userRole(adminRole)
			.adminRegion(region)
			.userCompany(userCompany)
			.adminEmail(requestDto.getUserEmail())
			.adminName(requestDto.getUserName())
			.adminPhone(requestDto.getUserPhone())
			.adminPwd(encodedPassword)
			.tokenVer(1)  // 초기 Token Version
			.build();

		// 9. 데이터베이스에 저장
		Admin savedAdmin = adminRepository.save(user);
		log.info("관리자 계정 DB 저장 완료: email={}", user.getAdminEmail());

		// 10. 환영 이메일 발송 (임시 비밀번호 포함)
		emailService.sendAdminWelcomeEmail(requestDto.getUserEmail(), temporaryPassword);

		// 11. 응답 DTO 생성 및 반환
		return AdminSignupResponseDto.fromEntity(savedAdmin);
	}

	/**
	 * 지역 조회 또는 생성
	 * - 지역명으로 기존 지역 조회
	 * - 존재하지 않으면 새로운 지역 생성
	 * - 관리자 회원가입 시 담당 지역 설정에 사용
	 *
	 * @param regionName 조회 또는 생성할 지역명
	 * @return AdminRegion 엔티티 (기존 또는 새로 생성된)
	 */
	private AdminRegion findOrCreateRegion(String regionName) {
		return adminRegionRepository.findByRegionName(regionName)
			.orElseGet(() -> {
				log.info("새로운 지역 정보 생성: {}", regionName);

				// regionName이 null인 경우 null 반환 (마스터 관리자)
				if (regionName == null) {
					return null;
				}

				// 새로운 AdminRegion 엔티티 생성
				AdminRegion newRegion = AdminRegion.builder()
					.regionName(regionName)
					.build();

				return adminRegionRepository.save(newRegion);
			});
	}

	/**
	 * 무작위 임시 비밀번호 생성
	 * - 10자리 영문 대소문자 + 숫자 조합
	 * - SecureRandom을 사용하여 암호학적으로 안전한 난수 생성
	 * - 비밀번호 초기화 시 사용
	 *
	 * 생성 규칙:
	 * - 길이: 10자
	 * - 문자 집합: A-Z, a-z, 0-9
	 * - SecureRandom으로 예측 불가능한 비밀번호 생성
	 *
	 * @return 생성된 임시 비밀번호 문자열
	 */
	private String generateRandomPassword() {
		// 사용 가능한 문자 집합
		final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

		// SecureRandom 인스턴스 생성
		SecureRandom random = new SecureRandom();

		// 10자리 무작위 문자열 생성
		return IntStream.range(0, 10)
			.map(i -> random.nextInt(chars.length()))
			.mapToObj(chars::charAt)
			.map(Object::toString)
			.collect(Collectors.joining());
	}

	/**
	 * 초기 마스터 관리자 계정 생성
	 * - 애플리케이션 시작 시 자동으로 호출 (DataSeedRunner)
	 * - 마스터 관리자 역할(roleId=0)이 없으면 생성
	 * - 마스터 관리자 계정이 없으면 생성
	 * - application.properties의 설정값 사용
	 *
	 * 처리 흐름:
	 * 1. 마스터 관리자 역할(roleId=0) 존재 여부 확인
	 * 2. 역할이 없으면 "Master" 역할 생성
	 * 3. 마스터 관리자 계정 존재 여부 확인
	 * 4. 계정이 없으면 application.properties 설정값으로 생성
	 * 5. 소속 기업 설정 (기본: "신한금융희망재단")
	 *
	 * 보안 고려사항:
	 * - 마스터 관리자 비밀번호는 환경 변수로 관리 권장
	 * - 초기 설정 후 비밀번호 변경 권장
	 */
	public void createInitialAdmin() {
		// 1. 마스터 관리자 역할(roleId=0) 존재 여부 확인
		Optional<UserRole> adminRoleOpt = userRoleRepository.findById(0);
		UserRole adminRole;

		// 2. 역할이 없으면 생성
		if (adminRoleOpt.isEmpty()) {
			log.info("역할(ID:0, 이름:Master)이 없어 새로 생성합니다.");
			UserRole newAdminRole = new UserRole("Master");
			newAdminRole.setRoleId(0);
			adminRole = userRoleRepository.save(newAdminRole);
		} else {
			adminRole = adminRoleOpt.get();
		}

		// 3. 마스터 관리자 계정 존재 여부 확인
		if (!adminRepository.existsByUserRole(adminRole)) {
			log.info("최초 관리자 계정이 존재하지 않아 새로 생성합니다.");

			// 4. 이메일 중복 확인
			if (adminRepository.existsByAdminEmail(masterEmail)) {
				log.info("이미 {} 계정이 존재하여 생성을 건너뜁니다.", masterEmail);
				return;
			}

			// 5. 소속 기업 조회 또는 생성
			UserCompany userCompany = getOrCreateCompany("신한금융희망재단");

			// 6. 마스터 관리자 엔티티 생성
			Admin admin = Admin.builder()
				.adminEmail(masterEmail)
				.adminName(masterName)
				.adminPwd(passwordService.encodePassword(masterPassword))
				.adminPhone(masterPhone)
				.tokenVer(1)
				.userCompany(userCompany)
				.userRole(adminRole)
				.build();

			// 7. 데이터베이스에 저장
			adminRepository.save(admin);
			log.info("최초 관리자 계정({}) 생성이 완료되었습니다.", masterEmail);
		}
	}
}
