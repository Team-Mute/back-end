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
import Team_Mute.back_end.domain.member.dto.request.SignupRequestDto;
import Team_Mute.back_end.domain.member.dto.request.UserInfoUpdateRequestDto;
import Team_Mute.back_end.domain.member.dto.response.AdminInfoResponse;
import Team_Mute.back_end.domain.member.dto.response.SignupResponseDto;
import Team_Mute.back_end.domain.member.dto.response.UserInfoResponseDto;
import Team_Mute.back_end.domain.member.entity.AdminRegion;
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
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

	@PostConstruct
	public void init() {
		createInitialAdmin();
	}

	public SignupResponseDto signUp(SignupRequestDto requestDto) {
		try {
			log.info("회원가입 요청: {}", requestDto.getUserEmail());

			if (userRepository.existsByUserEmail(requestDto.getUserEmail())) {
				throw new DuplicateEmailException("이미 가입된 이메일 입니다.");
			}

			UserCompany userCompany = getOrCreateCompany(requestDto.getCompanyName());
			UserRole userRole = userRoleRepository.findById(3)
				.orElseGet(() -> {
					log.info("기본 'customer' 역할(ID: 3)이 없어 새로 생성합니다.");
					UserRole newRole = new UserRole();
					newRole.setRoleId(3); // ID를 직접 설정
					newRole.setRoleName("customer");
					return userRoleRepository.save(newRole);
				});

			String encodedPassword = passwordService.encodePassword(requestDto.getUserPwd());

			User user = createUser(requestDto, encodedPassword, userCompany, userRole);
			User savedUser = userRepository.save(user);

			log.info("회원가입 완료: userId={}, email={}, company={}",
				savedUser.getUserId(), savedUser.getUserEmail(), savedUser.getUserCompany().getCompanyId());

			return new SignupResponseDto(
				requestDto.getUserName() + "님 회원가입이 완료 되었습니다.",
				savedUser.getUserId()
			);
		} catch (DuplicateEmailException e) {
			throw e;
		} catch (Exception e) {
			log.error("User registration failed for email: {}", requestDto.getUserEmail(), e);
			throw new UserRegistrationException("회원가입 처리 중 오류가 발생했습니다.");
		}
	}

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

	private User createUser(SignupRequestDto requestDto, String encodedPassword, UserCompany companyObject,
		UserRole userRoleObject) {
		return User.builder()
			.userName(requestDto.getUserName())
			.userPhone(requestDto.getUserPhone())
			.userEmail(requestDto.getUserEmail())
			.userPwd(encodedPassword)
			.userCompany(companyObject)
			.userRole(userRoleObject)
			.agreeEmail(requestDto.getAgreeEmail())
			.agreeSms(requestDto.getAgreeSms())
			.agreeLocation(requestDto.getAgreeLocation())
			.regDate(LocalDateTime.now())
			.tokenVer(1)
			.build();
	}

	@Transactional(readOnly = true)
	public UserInfoResponseDto getUserInfo(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);
		return UserInfoResponseDto.fromEntity(user);
	}

	public void updateUserInfo(Long userId, UserInfoUpdateRequestDto requestDto) {
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		if (!user.getUserEmail().equals(requestDto.getUserEmail())) {
			if (userRepository.existsByUserEmail(requestDto.getUserEmail())) {
				throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
			}
			user.setUserEmail(requestDto.getUserEmail());
		}
		user.setUserName(requestDto.getUserName());
		user.setUserPhone(requestDto.getUserPhone());
		user.setAgreeEmail(requestDto.getAgreeEmail());
		user.setAgreeSms(requestDto.getAgreeSms());
		user.setAgreeLocation(requestDto.getAgreeLocation());
		log.info("회원 정보 수정 완료: userId={}", userId);
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
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);
		userRepository.delete(user);
		log.info("DB 회원 정보 삭제 완료: userId={}", userId);
	}

	public void updatePassword(Long userId, PasswordUpdateRequestDto requestDto) {
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		if (!passwordService.matches(requestDto.getPassword(), user.getUserPwd())) {
			throw new InvalidPasswordException();
		}
		if (!passwordService.isStrongPassword(requestDto.getNewPassword())) {
			throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.");
		}
		user.setUserPwd(passwordService.encodePassword(requestDto.getNewPassword()));
		user.setTokenVer(user.getTokenVer() + 1);
		log.info("비밀번호 수정 및 토큰 버전 증가 완료: userId={}", userId);
	}

	@Transactional(readOnly = true)
	public boolean isEmailExists(String email) {
		return userRepository.existsByUserEmail(email);
	}

	@Transactional(readOnly = true)
	public Integer getTokenVer(Long userId) {
		return userRepository.findTokenVerByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
	}

	public void resetPassword(PasswordResetRequestDto requestDto) {
		User user = userRepository.findByUserEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		String temporaryPassword = generateRandomPassword();
		emailService.sendTemporaryPassword(user.getUserEmail(), temporaryPassword);
		user.setUserPwd(passwordService.encodePassword(temporaryPassword));
		user.setTokenVer(user.getTokenVer() + 1);
		log.info("비밀번호 초기화 및 DB 업데이트 완료: userId={}", user.getUserId());
	}

	@Transactional(readOnly = true)
	public AdminInfoResponse getAdminInfo(Long adminId) {
		User admin = userRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		String regionName = "N/A";
		if (admin.getAdminRegion().getRegionId() != null && admin.getAdminRegion().getRegionId() != 0) {
			regionName = adminRegionRepository.findById(admin.getAdminRegion().getRegionId())
				.map(AdminRegion::getRegionName)
				.orElse("지역 정보 없음");
		} else if (admin.getAdminRegion().getRegionId() != null) {
			regionName = "Master";
		}

		return new AdminInfoResponse(
			admin.getUserRole().getRoleId(),
			regionName,
			admin.getUserEmail(),
			admin.getUserName(),
			admin.getUserPhone()
		);
	}

	public void updateAdminInfo(Long adminId, AdminAccountUpdateRequest requestDto) {
		User admin = userRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		if (!admin.getUserEmail().equals(requestDto.getUserEmail())) {
			if (userRepository.existsByUserEmail(requestDto.getUserEmail())) {
				throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
			}
			admin.setUserEmail(requestDto.getUserEmail());
		}

		AdminRegion region = findOrCreateRegion(requestDto.getRegionName());
		admin.setAdminRegion(region);
		admin.setUserName(requestDto.getUserName());
		admin.setUserPhone(requestDto.getUserPhone());

		log.info("관리자 정보 수정 완료: adminId={}", adminId);
	}

	public void deleteAdminAccount(Authentication authentication, AdminAccountDeleteRequest requestDto) {
		User caller = checkMasterAuthority(authentication);

		User targetUser = userRepository.findByUserEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		if (caller.getUserId().equals(targetUser.getUserId())) {
			throw new IllegalArgumentException("자신을 삭제할 수 없습니다.");
		}
		if (targetUser.getUserRole().getRoleId() == 0) {
			throw new AccessDeniedException("Master 관리자는 삭제할 수 없습니다.");
		}

		deleteUser(targetUser.getUserId());
	}

	public void updateAdminPassword(Long adminId, AdminPasswordUpdateRequest requestDto) {
		User admin = userRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		if (!passwordService.matches(requestDto.getPassword(), admin.getUserPwd())) {
			throw new InvalidPasswordException();
		}

		admin.setUserPwd(passwordService.encodePassword(requestDto.getNewPassword()));
		admin.setTokenVer(admin.getTokenVer() + 1);
		log.info("관리자 비밀번호 수정 완료: adminId={}", adminId);
	}

	public void resetAdminPassword(AdminPasswordResetRequest requestDto) {
		User user = userRepository.findByUserEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		if (user.getUserRole().getRoleId() > 2) {
			throw new AccessDeniedException("관리자 계정만 초기화할 수 있습니다.");
		}

		String temporaryPassword = generateRandomPassword();
		emailService.sendTemporaryPassword(user.getUserEmail(), temporaryPassword);
		user.setUserPwd(passwordService.encodePassword(temporaryPassword));
		user.setTokenVer(user.getTokenVer() + 1);
		log.info("관리자 비밀번호 초기화 및 DB 업데이트 완료: userId={}", user.getUserId());
	}

	public void updateAdminRole(Authentication authentication, AdminRoleUpdateRequest requestDto) {
		User caller = checkMasterAuthority(authentication);

		User targetUser = userRepository.findByUserEmail(requestDto.getUserEmail())
			.orElseThrow(UserNotFoundException::new);

		if (caller.getUserId().equals(targetUser.getUserId())) {
			throw new IllegalArgumentException("자신의 권한을 수정할 수 없습니다.");
		}

		UserRole roleToSet = userRoleRepository.findById(requestDto.getRoleId())
			.orElseThrow(() -> new EntityNotFoundException("해당 역할을 찾을 수 없습니다."));
		targetUser.setUserRole(roleToSet);
		targetUser.setTokenVer(targetUser.getTokenVer() + 1);
		log.info("관리자 권한 수정 완료: targetUserId={}, newRoleId={}", targetUser.getUserId(), requestDto.getRoleId());
	}

	private User checkMasterAuthority(Authentication authentication) {
		Long callerId = Long.valueOf((String)authentication.getPrincipal());
		User caller = userRepository.findById(callerId)
			.orElseThrow(UserNotFoundException::new);

		if (caller.getUserRole().getRoleId() != 0) {
			throw new AccessDeniedException("Master 관리자만 이 작업을 수행할 수 있습니다.");
		}
		return caller;
	}

	public void adminSignUp(AdminSignupRequestDto requestDto) {
		if (requestDto.getRoleId() != 1 && requestDto.getRoleId() != 2) {
			throw new IllegalArgumentException("관리자 계정 생성은 역할 ID 1 또는 2만 가능합니다.");
		}
		if (userRepository.existsByUserEmail(requestDto.getUserEmail())) {
			throw new DuplicateEmailException("이미 가입된 이메일 입니다.");
		}

		String temporaryPassword = generateRandomPassword();
		AdminRegion region = findOrCreateRegion(requestDto.getRegionName());
		String encodedPassword = passwordService.encodePassword(temporaryPassword);
		UserRole adminRole = userRoleRepository.findById(requestDto.getRoleId()).orElseThrow();

		User user = User.builder()
			.userRole(adminRole)
			.adminRegion(region)
			.userEmail(requestDto.getUserEmail())
			.userName(requestDto.getUserName())
			.userPhone(requestDto.getUserPhone())
			.userPwd(encodedPassword)
			.tokenVer(1)
			.build();

		userRepository.save(user);
		log.info("관리자 계정 DB 저장 완료: email={}", user.getUserEmail());

		emailService.sendAdminWelcomeEmail(requestDto.getUserEmail(), temporaryPassword);
	}

	private AdminRegion findOrCreateRegion(String regionName) {
		return adminRegionRepository.findByRegionName(regionName)
			.orElseGet(() -> {
				log.info("새로운 지역 정보 생성: {}", regionName);
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

		AdminRegion defaultRegion = adminRegionRepository.findByRegionName("전 지역")
			.orElseGet(() -> {
				log.info("'전 지역'이 존재하지 않아 새로 생성합니다.");
				return adminRegionRepository.save(new AdminRegion("전 지역"));
			});

		if (!userRepository.existsByUserRole(adminRole)) {
			log.info("최초 관리자 계정이 존재하지 않아 새로 생성합니다.");
			String adminEmail = "songh6508@gmail.com";

			if (userRepository.existsByUserEmail(adminEmail)) {
				log.info("이미 {} 계정이 존재하여 생성을 건너뜁니다.", adminEmail);
				return;
			}

			User admin = User.builder()
				.userEmail(adminEmail)
				.userName("master1")
				.userPwd(passwordService.encodePassword("master1234!"))
				.userPhone("01074181170")
				.tokenVer(1)
				.userRole(adminRole)
				.adminRegion(defaultRegion)
				.build();

			userRepository.save(admin);
			log.info("최초 관리자 계정({}) 생성이 완료되었습니다.", adminEmail);
		}
	}
}
