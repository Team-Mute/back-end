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
				savedUser.getUserId(),
				savedUser.getUserRole().getRoleId()
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
			.userEmail(requestDto.getUserEmail())
			.userPwd(encodedPassword)
			.userCompany(companyObject)
			.userRole(userRoleObject)
			.agreeEmail(requestDto.getAgreeEmail())
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
		user.setAgreeEmail(requestDto.getAgreeEmail());
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

	private String generateRandomPassword() {
		final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		SecureRandom random = new SecureRandom();
		return IntStream.range(0, 10)
			.map(i -> random.nextInt(chars.length()))
			.mapToObj(chars::charAt)
			.map(Object::toString)
			.collect(Collectors.joining());
	}
}
