package Team_Mute.back_end.domain.member.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Team_Mute.back_end.domain.member.dto.request.PasswordResetRequestDto;
import Team_Mute.back_end.domain.member.dto.request.PasswordUpdateRequestDto;
import Team_Mute.back_end.domain.member.dto.request.SignupRequestDto;
import Team_Mute.back_end.domain.member.dto.request.UserInfoUpdateRequestDto;
import Team_Mute.back_end.domain.member.dto.response.SignupResponseDto;
import Team_Mute.back_end.domain.member.dto.response.UserInfoResponseDto;
import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.entity.UserCompany;
import Team_Mute.back_end.domain.member.entity.UserRole;
import Team_Mute.back_end.domain.member.exception.DuplicateEmailException;
import Team_Mute.back_end.domain.member.exception.InvalidPasswordException;
import Team_Mute.back_end.domain.member.exception.UserNotFoundException;
import Team_Mute.back_end.domain.member.exception.UserRegistrationException;
import Team_Mute.back_end.domain.member.repository.AdminRegionRepository;
import Team_Mute.back_end.domain.member.repository.UserCompanyRepository;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.member.repository.UserRoleRepository;
import Team_Mute.back_end.domain.member.session.SessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일반 사용자 비즈니스 로직 서비스 클래스
 * 사용자 계정의 생명주기 전체를 관리하는 핵심 서비스
 * 공간 예약 서비스를 이용하는 일반 사용자(고객) 전용 서비스
 *
 * 주요 기능:
 * - 사용자 회원가입 (소속 기업 정보 필수)
 * - 사용자 정보 조회, 수정, 탈퇴 (CRUD)
 * - 비밀번호 변경 및 초기화 (임시 비밀번호 발송)
 * - 이메일 중복 확인
 * - Redis 세션 및 Token Version 관리
 *
 * 보안 기능:
 * - Token Version 증가로 기존 JWT 토큰 무효화
 * - Redis 세션 삭제로 즉시 로그아웃 처리
 * - BCrypt 암호화로 비밀번호 보호
 * - 이메일 중복 검증
 *
 * AdminService와의 차이점:
 * - roleId=3 (일반 회원)으로 고정
 * - 담당 지역 없음 (관리자만 해당)
 * - 권한 관리 기능 없음
 * - 소속 기업 정보 필수 입력
 *
 * @author Team Mute
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

	private final UserRepository userRepository;
	private final UserCompanyRepository userCompanyRepository;
	private final PasswordService passwordService;
	private final SessionStore sessionStore;
	private final EmailService emailService;
	private final AdminRegionRepository adminRegionRepository;
	private final UserRoleRepository userRoleRepository;

	/**
	 * 사용자 회원가입
	 * - 이메일 중복 확인 후 신규 사용자 계정 생성
	 * - 소속 기업 정보 필수 (없으면 새로 생성)
	 * - 비밀번호 BCrypt 암호화 저장
	 * - 기본 역할(roleId=3, customer) 자동 할당
	 *
	 * 처리 흐름:
	 * 1. 이메일 중복 확인 (이미 존재하면 예외 발생)
	 * 2. 소속 기업 조회 또는 생성 (getOrCreateCompany)
	 * 3. 사용자 역할 조회 또는 생성 (roleId=3, "customer")
	 * 4. 비밀번호 암호화 (BCrypt)
	 * 5. User 엔티티 생성 (createUser)
	 * 6. 데이터베이스에 저장
	 * 7. 회원가입 완료 응답 DTO 반환
	 *
	 * 데이터 검증:
	 * - SignupRequestDto의 @Valid 어노테이션으로 컨트롤러에서 1차 검증
	 * - 이메일 형식, 비밀번호 강도, 이름 형식 등
	 * - 서비스 레이어에서 이메일 중복 2차 검증
	 *
	 * 기본 설정:
	 * - tokenVer: 1 (초기값)
	 * - roleId: 3 (일반 회원)
	 * - regDate: 현재 시각 (LocalDateTime.now())
	 * - agreeEmail: 요청 DTO에서 전달받은 값 (이메일 수신 동의 여부)
	 *
	 * @param requestDto 회원가입 요청 DTO (이메일, 비밀번호, 이름, 소속 기업, 이메일 수신 동의)
	 * @return SignupResponseDto (환영 메시지, 생성된 사용자 ID, 역할 ID)
	 * @throws DuplicateEmailException 이미 가입된 이메일인 경우
	 * @throws UserRegistrationException 회원가입 처리 중 예외 발생 시
	 */
	public SignupResponseDto signUp(SignupRequestDto requestDto) {
		try {
			// 1. 회원가입 요청 로그
			log.info("회원가입 요청: {}", requestDto.getUserEmail());

			// 2. 이메일 중복 확인
			if (userRepository.existsByUserEmail(requestDto.getUserEmail())) {
				throw new DuplicateEmailException("이미 가입된 이메일 입니다.");
			}

			// 3. 소속 기업 조회 또는 생성
			// - 기업명으로 기존 기업 검색
			// - 존재하지 않으면 새로운 기업 생성 (자동 증가 ID 할당)
			UserCompany userCompany = getOrCreateCompany(requestDto.getCompanyName());

			// 4. 사용자 역할 조회 또는 생성
			// - roleId=3 ("customer", 일반 회원) 조회
			// - 존재하지 않으면 새로 생성 (최초 실행 시)
			UserRole userRole = userRoleRepository.findById(3)
				.orElseGet(() -> {
					log.info("기본 'customer' 역할(ID: 3)이 없어 새로 생성합니다.");
					UserRole newRole = new UserRole();
					newRole.setRoleId(3); // ID를 직접 설정
					newRole.setRoleName("customer");
					return userRoleRepository.save(newRole);
				});

			// 5. 비밀번호 암호화
			// - 평문 비밀번호를 BCrypt로 암호화
			// - 약 60자의 해시값 생성 (Salt 포함)
			String encodedPassword = passwordService.encodePassword(requestDto.getUserPwd());

			// 6. User 엔티티 생성
			User user = createUser(requestDto, encodedPassword, userCompany, userRole);

			// 7. 데이터베이스에 저장
			User savedUser = userRepository.save(user);

			// 8. 회원가입 완료 로그
			log.info("회원가입 완료: userId={}, email={}, company={}",
				savedUser.getUserId(), savedUser.getUserEmail(), savedUser.getUserCompany().getCompanyId());

			// 9. 응답 DTO 생성 및 반환
			return new SignupResponseDto(
				requestDto.getUserName() + "님 회원가입이 완료 되었습니다.",
				savedUser.getUserId(),
				savedUser.getUserRole().getRoleId()
			);
		} catch (DuplicateEmailException e) {
			// 이메일 중복 예외는 그대로 재발생
			throw e;
		} catch (Exception e) {
			// 기타 예외는 UserRegistrationException으로 래핑
			log.error("User registration failed for email: {}", requestDto.getUserEmail(), e);
			throw new UserRegistrationException("회원가입 처리 중 오류가 발생했습니다.");
		}
	}

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
	 * Company ID 생성 로직:
	 * - findMaxCompanyId()로 현재 최대값 조회
	 * - 데이터가 없으면 0 반환 (초기 상태)
	 * - 최대값 + 1을 새 Company ID로 사용
	 *
	 * @param companyName 생성할 기업명
	 * @return 저장된 UserCompany 엔티티
	 * @throws UserRegistrationException 기업 생성 중 오류 발생 시
	 */
	private UserCompany createNewCompany(String companyName) {
		try {
			// 1. 최대 Company ID 조회 (데이터가 없으면 0)
			Integer maxCompanyId = userCompanyRepository.findMaxCompanyId().orElse(0);

			// 2. 새로운 기업 엔티티 생성
			UserCompany newCompany = UserCompany.builder()
				.companyId(maxCompanyId + 1)      // 최대값 + 1
				.companyName(companyName)
				.regDate(LocalDateTime.now())
				.build();

			// 3. 데이터베이스에 저장
			UserCompany savedCompany = userCompanyRepository.save(newCompany);

			log.info("New company created: {}", companyName);
			return savedCompany;
		} catch (Exception e) {
			log.error("Failed to create new company: {}", companyName, e);
			throw new UserRegistrationException("회사 정보 처리 중 오류가 발생했습니다.");
		}
	}

	/**
	 * User 엔티티 생성
	 * - 회원가입 요청 DTO와 암호화된 비밀번호로 User 엔티티 생성
	 * - Builder 패턴을 사용하여 가독성 향상
	 * - 초기 Token Version은 1로 설정
	 *
	 * @param requestDto 회원가입 요청 DTO
	 * @param encodedPassword 암호화된 비밀번호
	 * @param companyObject 소속 기업 엔티티
	 * @param userRoleObject 사용자 역할 엔티티
	 * @return 생성된 User 엔티티 (아직 저장되지 않음)
	 */
	private User createUser(SignupRequestDto requestDto, String encodedPassword, UserCompany companyObject,
		UserRole userRoleObject) {
		return User.builder()
			.userName(requestDto.getUserName())           // 사용자 이름
			.userEmail(requestDto.getUserEmail())         // 이메일 주소
			.userPwd(encodedPassword)                     // 암호화된 비밀번호
			.userCompany(companyObject)                   // 소속 기업
			.userRole(userRoleObject)                     // 사용자 역할 (roleId=3)
			.agreeEmail(requestDto.getAgreeEmail())       // 이메일 수신 동의 여부
			.regDate(LocalDateTime.now())                 // 등록 일시
			.tokenVer(1)                                  // 초기 Token Version
			.build();
	}

	/**
	 * 사용자 정보 조회
	 * - 읽기 전용 트랜잭션으로 성능 최적화
	 * - 사용자 ID로 상세 정보 조회
	 * - UserController의 getUserInfo API에서 호출
	 *
	 * @param userId 조회할 사용자 ID
	 * @return UserInfoResponseDto (사용자 ID, 역할, 기업, 이메일, 이름, 등록/수정 일시, 이메일 수신 동의)
	 * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
	 */
	@Transactional(readOnly = true)
	public UserInfoResponseDto getUserInfo(Long userId) {
		// 1. 사용자 조회
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		// 2. UserInfoResponseDto로 변환 및 반환
		// - fromEntity() 정적 팩토리 메서드 사용
		return UserInfoResponseDto.fromEntity(user);
	}

	/**
	 * 사용자 정보 수정
	 * - 이메일, 이름, 이메일 수신 동의 여부 수정 가능
	 * - 이메일 변경 시 중복 검증
	 *
	 * 처리 흐름:
	 * 1. 사용자 조회
	 * 2. 이메일 변경 시 중복 체크
	 * 3. 이름 및 이메일 수신 동의 수정
	 *
	 * 주의사항:
	 * - 비밀번호는 별도의 updatePassword() 메서드로 변경
	 * - 소속 기업은 변경 불가 (비즈니스 정책)
	 * - 역할은 변경 불가 (일반 사용자 고정)
	 *
	 * @param userId 수정할 사용자 ID
	 * @param requestDto 수정할 정보 DTO
	 * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
	 * @throws DuplicateEmailException 변경하려는 이메일이 이미 사용 중인 경우
	 */
	public void updateUserInfo(Long userId, UserInfoUpdateRequestDto requestDto) {
		// 1. 사용자 조회
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		// 2. 이메일 변경 시 중복 체크
		if (!user.getUserEmail().equals(requestDto.getUserEmail())) {
			if (userRepository.existsByUserEmail(requestDto.getUserEmail())) {
				throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
			}
			user.setUserEmail(requestDto.getUserEmail());
		}

		// 3. 이름 수정
		user.setUserName(requestDto.getUserName());

		// 4. 이메일 수신 동의 여부 수정
		user.setAgreeEmail(requestDto.getAgreeEmail());

		log.info("회원 정보 수정 완료: userId={}", userId);
	}

	/**
	 * 사용자 탈퇴
	 * - Redis에 저장된 모든 세션 삭제 (즉시 로그아웃)
	 * - Refresh Token 블랙리스트 등록
	 * - 데이터베이스에서 사용자 정보 삭제
	 *
	 * 처리 흐름:
	 * 1. Redis에서 해당 사용자의 모든 세션 ID 조회
	 * 2. 각 세션의 Refresh Token JTI 조회
	 * 3. Refresh Token을 블랙리스트에 등록 (7일간 유지)
	 * 4. Redis에서 세션 정보 삭제
	 * 5. 데이터베이스에서 사용자 엔티티 삭제
	 *
	 * 보안 고려사항:
	 * - 탈퇴 즉시 모든 기기에서 로그아웃 처리
	 * - Refresh Token 무효화로 재로그인 방지
	 * - 삭제된 사용자 데이터는 복구 불가 (하드 삭제)
	 *
	 * 개선 가능 사항:
	 * - Soft Delete 구현 (deleted_at 컬럼 추가)
	 * - 탈퇴 사유 수집
	 * - 재가입 제한 기간 설정
	 *
	 * @param userId 탈퇴할 사용자 ID
	 * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
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

		// 6. 데이터베이스에서 사용자 조회
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		// 7. 데이터베이스에서 사용자 삭제
		userRepository.delete(user);
		log.info("DB 회원 정보 삭제 완료: userId={}", userId);
	}

	/**
	 * 사용자 비밀번호 변경
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
	 * @param userId 비밀번호를 변경할 사용자 ID
	 * @param requestDto 비밀번호 변경 요청 DTO (기존 비밀번호, 새 비밀번호)
	 * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
	 * @throws InvalidPasswordException 기존 비밀번호가 일치하지 않는 경우
	 * @throws IllegalArgumentException 새 비밀번호가 강도 요구사항을 충족하지 않는 경우
	 */
	public void updatePassword(Long userId, PasswordUpdateRequestDto requestDto) {
		// 1. 사용자 조회
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		// 2. 기존 비밀번호 검증
		if (!passwordService.matches(requestDto.getPassword(), user.getUserPwd())) {
			throw new InvalidPasswordException();
		}

		// 3. 새 비밀번호 강도 검증
		if (!passwordService.isStrongPassword(requestDto.getNewPassword())) {
			throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.");
		}

		// 4. 새 비밀번호 암호화 및 저장
		user.setUserPwd(passwordService.encodePassword(requestDto.getNewPassword()));

		// 5. Token Version 증가 (기존 JWT 토큰 무효화)
		user.setTokenVer(user.getTokenVer() + 1);
	}

	/**
	 * 이메일 존재 여부 확인
	 * - 읽기 전용 트랜잭션으로 성능 최적화
	 * - 회원가입 시 이메일 중복 체크에 사용
	 * - UserController의 checkEmailDuplication API에서 호출
	 *
	 * @param email 확인할 이메일 주소
	 * @return 존재하면 true, 존재하지 않으면 false
	 */
	@Transactional(readOnly = true)
	public boolean isEmailExists(String email) {
		return userRepository.existsByUserEmail(email);
	}

	/**
	 * Token Version 조회
	 * - 읽기 전용 트랜잭션으로 성능 최적화
	 * - JWT Refresh Token 재발급 시 Token Version 검증에 사용
	 * - AuthService의 refresh 메서드에서 호출
	 *
	 * @param userId 조회할 사용자 ID
	 * @return Token Version
	 * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
	 */
	@Transactional(readOnly = true)
	public Integer getTokenVer(Long userId) {
		return userRepository.findTokenVerByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
	}

	/**
	 * 사용자 비밀번호 초기화
	 * - 임시 비밀번호 생성 (10자리 무작위 문자열)
	 * - 이메일로 임시 비밀번호 발송
	 * - 임시 비밀번호를 BCrypt 암호화하여 저장
	 * - Token Version 증가로 기존 JWT 토큰 무효화
	 *
	 * 처리 흐름:
	 * 1. 이메일로 사용자 조회
	 * 2. 10자리 무작위 임시 비밀번호 생성 (generateRandomPassword)
	 * 3. 임시 비밀번호를 이메일로 발송 (EmailService)
	 * 4. 임시 비밀번호를 암호화하여 데이터베이스에 저장
	 * 5. Token Version 증가
	 *
	 * 보안 고려사항:
	 * - 임시 비밀번호는 SecureRandom으로 생성하여 예측 불가
	 * - 로그인 후 비밀번호 변경 유도
	 * - Token Version 증가로 기존 세션 무효화
	 *
	 * @param requestDto 비밀번호 초기화 요청 DTO (이메일 주소)
	 * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
	 */
	public void resetPassword(PasswordResetRequestDto requestDto) {
		// 1. 이메일로 사용자 조회
		User user = userRepository.findByUserEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		// 2. 10자리 무작위 임시 비밀번호 생성
		String temporaryPassword = generateRandomPassword();

		// 3. 임시 비밀번호를 이메일로 발송
		emailService.sendTemporaryPassword(user.getUserEmail(), temporaryPassword);

		// 4. 임시 비밀번호를 암호화하여 저장
		user.setUserPwd(passwordService.encodePassword(temporaryPassword));

		// 5. Token Version 증가 (기존 JWT 토큰 무효화)
		user.setTokenVer(user.getTokenVer() + 1);
	}

	/**
	 * 무작위 임시 비밀번호 생성
	 * - 10자리 영문 대소문자 + 숫자 조합
	 * - SecureRandom을 사용하여 암호학적으로 안전한 난수 생성
	 * - 비밀번호 초기화 시 사용
	 *
	 * 생성 규칙:
	 * - 길이: 10자
	 * - 문자 집합: A-Z, a-z, 0-9 (62개)
	 * - SecureRandom으로 예측 불가능한 비밀번호 생성
	 *
	 * 보안 강화 방안:
	 * - 특수문자 추가 (현재는 영문+숫자만)
	 * - 길이 증가 (10자 → 12자 이상)
	 * - 최소 복잡도 보장 (영문, 숫자 각각 최소 1개씩)
	 *
	 * @return 생성된 임시 비밀번호 문자열 (10자)
	 */
	private String generateRandomPassword() {
		// 사용 가능한 문자 집합 (영문 대소문자 + 숫자)
		final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

		// SecureRandom 인스턴스 생성
		SecureRandom random = new SecureRandom();

		// 10자리 무작위 문자열 생성
		return IntStream.range(0, 10)
			.map(i -> random.nextInt(chars.length()))  // 0 ~ 61 범위의 무작위 인덱스
			.mapToObj(chars::charAt)                    // 인덱스로 문자 추출
			.map(Object::toString)                      // 문자를 문자열로 변환
			.collect(Collectors.joining());             // 하나의 문자열로 결합
	}
}
