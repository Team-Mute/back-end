package Team_Mute.back_end.domain.member.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

	private final BCryptPasswordEncoder passwordEncoder;

	public String encodePassword(String rawPassword) {
		if (rawPassword == null || rawPassword.trim().isEmpty()) {
			throw new IllegalArgumentException("비밀번호는 null이거나 빈 값일 수 없습니다.");
		}

		String encodedPassword = passwordEncoder.encode(rawPassword);
		log.debug("Password encoded successfully");
		return encodedPassword;
	}

	public boolean matches(String rawPassword, String encodedPassword) {
		if (rawPassword == null || encodedPassword == null) {
			return false;
		}

		boolean isMatch = passwordEncoder.matches(rawPassword, encodedPassword);
		log.debug("Password match result: {}", isMatch);
		return isMatch;
	}

	public boolean isStrongPassword(String password) {
		if (password == null || password.length() < 8) {
			return false;
		}
		boolean hasLetter = password.chars().anyMatch(c ->
			(c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
		boolean hasDigit = password.chars().anyMatch(Character::isDigit);
		boolean hasSpecialChar = password.chars().anyMatch(c ->
			"@$!%*?&".indexOf(c) >= 0);

		return hasLetter && hasDigit && hasSpecialChar;
	}
}
