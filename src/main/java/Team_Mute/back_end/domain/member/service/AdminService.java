package Team_Mute.back_end.domain.member.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

	private final UserRepository userRepository;
	private final AdminRepository adminRepository;
	private final UserCompanyRepository userCompanyRepository;
	private final PasswordService passwordService;
	private final SessionStore sessionStore;
	private final EmailService emailService;
	private final AdminRegionRepository adminRegionRepository;
	private final UserRoleRepository userRoleRepository;

	private UserCompany getOrCreateCompany(String companyName) {
		return userCompanyRepository.findByCompanyName(companyName)
			.orElseGet(() -> createNewCompany(companyName));
	}

	private UserCompany createNewCompany(String companyName) {
		try {
			Integer maxCompanyId = userCompanyRepository.findMaxCompanyId().orElse(0);
			UserCompany newCompany = UserCompany.builder()
				.companyId(maxCompanyId + 1)
				.companyName(companyName)
				.regDate(LocalDateTime.now())
				.build();
			UserCompany savedCompany = userCompanyRepository.save(newCompany);
			log.info("New company created: {}", companyName);
			return savedCompany;
		} catch (Exception e) {
			log.error("Failed to create new company: {}", companyName, e);
			throw new UserRegistrationException("회사 정보 처리 중 오류가 발생했습니다.");
		}
	}

	public void deleteUser(Long userId) {
		Set<String> sessionIds = sessionStore.getUserSids(userId.toString());
		if (sessionIds != null && !sessionIds.isEmpty()) {
			for (String sid : sessionIds) {
				String rtJti = sessionStore.getCurrentRtJti(sid);
				if (rtJti != null) {
					sessionStore.revokeRt(rtJti, java.time.Duration.ofDays(7));
				}
				sessionStore.deleteSession(sid, userId.toString());
			}
			log.info("Redis 세션 삭제 완료: userId={}", userId);
		}
		Admin user = adminRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);
		adminRepository.delete(user);
		log.info("DB 회원 정보 삭제 완료: userId={}", userId);
	}

	public void updatePassword(Long userId, PasswordUpdateRequestDto requestDto) {
		Admin user = adminRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		if (!passwordService.matches(requestDto.getPassword(), user.getAdminPwd())) {
			throw new InvalidPasswordException();
		}
		if (!passwordService.isStrongPassword(requestDto.getNewPassword())) {
			throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.");
		}
		user.setAdminPwd(passwordService.encodePassword(requestDto.getNewPassword()));
		user.setTokenVer(user.getTokenVer() + 1);
		log.info("비밀번호 수정 및 토큰 버전 증가 완료: userId={}", userId);
	}

	@Transactional(readOnly = true)
	public boolean isEmailExists(String email) {
		return adminRepository.existsByAdminEmail(email);
	}

	@Transactional(readOnly = true)
	public Integer getTokenVer(Long userId) {
		return adminRepository.findTokenVerByAdminId(userId)
			.orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
	}

	public void resetPassword(PasswordResetRequestDto requestDto) {
		Admin user = adminRepository.findByAdminEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		String temporaryPassword = generateRandomPassword();
		emailService.sendTemporaryPassword(user.getAdminEmail(), temporaryPassword);
		user.setAdminPwd(passwordService.encodePassword(temporaryPassword));
		user.setTokenVer(user.getTokenVer() + 1);
		log.info("비밀번호 초기화 및 DB 업데이트 완료: userId={}", user.getAdminId());
	}

	@Transactional(readOnly = true)
	public AdminInfoResponse getAdminInfo(Long adminId) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		String regionName = "N/A";
		if (admin.getAdminRegion() != null && admin.getAdminRegion().getRegionId() != null) {
			regionName = adminRegionRepository.findById(admin.getAdminRegion().getRegionId())
				.map(AdminRegion::getRegionName)
				.orElse("지역 정보 없음");
		} else {
			regionName = "Master";
		}

		return new AdminInfoResponse(
			admin.getUserRole().getRoleId(),
			regionName,
			admin.getAdminEmail(),
			admin.getAdminName(),
			admin.getAdminPhone()
		);
	}

	public void updateAdminInfo(Long adminId, AdminAccountUpdateRequest requestDto) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		if (!admin.getAdminEmail().equals(requestDto.getUserEmail())) {
			if (adminRepository.existsByAdminEmail(requestDto.getUserEmail())) {
				throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
			}
			admin.setAdminEmail(requestDto.getUserEmail());
		}

		if (requestDto.getRegionName() != null) {
			AdminRegion region = findOrCreateRegion(requestDto.getRegionName());
			admin.setAdminRegion(region);
		}
		admin.setAdminName(requestDto.getUserName());
		admin.setAdminPhone(requestDto.getUserPhone());

		log.info("관리자 정보 수정 완료: adminId={}", adminId);
	}

	public void deleteAdminAccount(Authentication authentication, AdminAccountDeleteRequest requestDto) {
		Admin caller = checkMasterAuthority(authentication);

		Admin targetUser = adminRepository.findByAdminEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		if (caller.getAdminId().equals(targetUser.getAdminId())) {
			throw new IllegalArgumentException("자신을 삭제할 수 없습니다.");
		}
		if (targetUser.getUserRole().getRoleId() == 0) {
			throw new AccessDeniedException("Master 관리자는 삭제할 수 없습니다.");
		}

		deleteUser(targetUser.getAdminId());
	}

	public void updateAdminPassword(Long adminId, AdminPasswordUpdateRequest requestDto) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		if (!passwordService.matches(requestDto.getPassword(), admin.getAdminPwd())) {
			throw new InvalidPasswordException();
		}

		admin.setAdminPwd(passwordService.encodePassword(requestDto.getNewPassword()));
		admin.setTokenVer(admin.getTokenVer() + 1);
		log.info("관리자 비밀번호 수정 완료: adminId={}", adminId);
	}

	public void resetAdminPassword(AdminPasswordResetRequest requestDto) {
		Admin user = adminRepository.findByAdminEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		if (user.getUserRole().getRoleId() > 2) {
			throw new AccessDeniedException("관리자 계정만 초기화할 수 있습니다.");
		}

		String temporaryPassword = generateRandomPassword();
		emailService.sendTemporaryPassword(user.getAdminEmail(), temporaryPassword);
		user.setAdminPwd(passwordService.encodePassword(temporaryPassword));
		user.setTokenVer(user.getTokenVer() + 1);
		log.info("관리자 비밀번호 초기화 및 DB 업데이트 완료: userId={}", user.getAdminId());
	}

	public void updateAdminRole(Authentication authentication, AdminRoleUpdateRequest requestDto) {
		Admin caller = checkMasterAuthority(authentication);

		Admin targetUser = adminRepository.findByAdminEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		if (caller.getAdminId().equals(targetUser.getAdminId())) {
			throw new IllegalArgumentException("자신의 권한을 수정할 수 없습니다.");
		}

		UserRole roleToSet = userRoleRepository.findById(requestDto.getRoleId())
			.orElseThrow(() -> new EntityNotFoundException("해당 역할을 찾을 수 없습니다."));
		targetUser.setUserRole(roleToSet);
		targetUser.setTokenVer(targetUser.getTokenVer() + 1);
		log.info("관리자 권한 수정 완료: targetUserId={}, newRoleId={}", targetUser.getAdminId(), requestDto.getRoleId());
	}

	private Admin checkMasterAuthority(Authentication authentication) {
		Long callerId = Long.valueOf((String)authentication.getPrincipal());
		Admin caller = adminRepository.findById(callerId)
			.orElseThrow(UserNotFoundException::new);

		if (caller.getUserRole().getRoleId() != 0) {
			throw new AccessDeniedException("Master 관리자만 이 작업을 수행할 수 있습니다.");
		}
		return caller;
	}

	public AdminSignupResponseDto adminSignUp(AdminSignupRequestDto requestDto) {
		if (requestDto.getRoleId() != 1 && requestDto.getRoleId() != 2) {
			throw new IllegalArgumentException("관리자 계정 생성은 역할 ID 1 또는 2만 가능합니다.");
		}
		if (adminRepository.existsByAdminEmail(requestDto.getUserEmail())) {
			throw new DuplicateEmailException("이미 가입된 이메일 입니다.");
		}

		String temporaryPassword = generateRandomPassword();
		AdminRegion region = findOrCreateRegion(requestDto.getRegionName());
		String encodedPassword = passwordService.encodePassword(temporaryPassword);
		UserCompany userCompany = getOrCreateCompany("신한금융희망재단");
		UserRole adminRole = userRoleRepository.findById(requestDto.getRoleId()).orElseThrow();

		Admin user = Admin.builder()
			.userRole(adminRole)
			.adminRegion(region)
			.userCompany(userCompany)
			.adminEmail(requestDto.getUserEmail())
			.adminName(requestDto.getUserName())
			.adminPhone(requestDto.getUserPhone())
			.adminPwd(encodedPassword)
			.tokenVer(1)
			.build();

		Admin savedAdmin = adminRepository.save(user);
		log.info("관리자 계정 DB 저장 완료: email={}", user.getAdminEmail());

		emailService.sendAdminWelcomeEmail(requestDto.getUserEmail(), temporaryPassword);
		return AdminSignupResponseDto.fromEntity(savedAdmin);
	}

	private AdminRegion findOrCreateRegion(String regionName) {
		return adminRegionRepository.findByRegionName(regionName)
			.orElseGet(() -> {
				log.info("새로운 지역 정보 생성: {}", regionName);
				if (regionName == null) {
					return null;
				}
				AdminRegion newRegion = AdminRegion.builder()
					.regionName(regionName)
					.build();
				return adminRegionRepository.save(newRegion);
			});
	}

	private String generateRandomPassword() {
		final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		SecureRandom random = new SecureRandom();
		return IntStream.range(0, 10)
			.map(i -> random.nextInt(chars.length()))
			.mapToObj(chars::charAt)
			.map(Object::toString)
			.collect(Collectors.joining());
	}

	public void createInitialAdmin() {
		Optional<UserRole> adminRoleOpt = userRoleRepository.findById(0);
		UserRole adminRole;
		if (adminRoleOpt.isEmpty()) {
			log.info("역할(ID:0, 이름:Master)이 없어 새로 생성합니다.");
			UserRole newAdminRole = new UserRole("Master");
			newAdminRole.setRoleId(0);
			adminRole = userRoleRepository.save(newAdminRole);
		} else {
			adminRole = adminRoleOpt.get();
		}

		if (!adminRepository.existsByUserRole(adminRole)) {
			log.info("최초 관리자 계정이 존재하지 않아 새로 생성합니다.");
			String adminEmail = "songh6508@gmail.com";

			if (adminRepository.existsByAdminEmail(adminEmail)) {
				log.info("이미 {} 계정이 존재하여 생성을 건너뜁니다.", adminEmail);
				return;
			}

			UserCompany userCompany = getOrCreateCompany("신한금융희망재단");

			Admin admin = Admin.builder()
				.adminEmail(adminEmail)
				.adminName("master1")
				.adminPwd(passwordService.encodePassword("master1234!"))
				.adminPhone("01074181170")
				.tokenVer(1)
				.userCompany(userCompany)
				.userRole(adminRole)
				.build();

			adminRepository.save(admin);
			log.info("최초 관리자 계정({}) 생성이 완료되었습니다.", adminEmail);
		}
	}
}
