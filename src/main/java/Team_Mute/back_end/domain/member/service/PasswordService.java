package Team_Mute.back_end.domain.member.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 비밀번호 암호화 및 검증 서비스 클래스
 * BCryptPasswordEncoder를 사용하여 비밀번호의 안전한 저장 및 검증 기능 제공
 * Spring Security에서 제공하는 BCrypt 해싱 알고리즘 활용
 *
 * 주요 기능:
 * - 평문 비밀번호를 BCrypt로 암호화 (Salt 자동 적용)
 * - 평문 비밀번호와 암호화된 비밀번호 일치 여부 검증
 * - 비밀번호 강도 검증 (영문, 숫자, 특수문자 포함 8자 이상)
 *
 * BCrypt 특징:
 * - 단방향 해시 함수 (복호화 불가능)
 * - 랜덤 Salt 자동 적용으로 같은 비밀번호도 매번 다른 해시값 생성
 * - 느린 연산으로 무차별 대입 공격(Brute Force)에 강함
 * - 해시 강도(strength) 조절 가능 (기본값: 10)
 *
 * 보안 고려사항:
 * - 비밀번호는 절대 평문으로 저장하지 않음
 * - 로그인 시 matches() 메서드로 비교 (직접 비교 금지)
 * - 디버그 로그에 실제 비밀번호 값 출력 금지
 *
 * @author Team Mute
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

	private final BCryptPasswordEncoder passwordEncoder;

	/**
	 * 평문 비밀번호를 BCrypt로 암호화
	 * - 랜덤 Salt를 자동으로 생성하여 적용
	 * - 같은 비밀번호를 암호화해도 매번 다른 해시값 생성
	 * - 회원가입, 비밀번호 변경 시 사용
	 *
	 * BCrypt 암호화 과정:
	 * 1. 랜덤 Salt 생성 (매번 다른 값)
	 * 2. 평문 비밀번호와 Salt를 결합
	 * 3. BCrypt 해싱 알고리즘 적용 (여러 라운드 반복)
	 * 4. Salt와 해시값을 함께 저장 (하나의 문자열로)
	 *
	 * 사용 예시:
	 * - AdminService.adminSignUp(): 관리자 회원가입 시 임시 비밀번호 암호화
	 * - UserService.signUp(): 사용자 회원가입 시 비밀번호 암호화
	 * - AdminService.updatePassword(): 비밀번호 변경 시 새 비밀번호 암호화
	 *
	 * @param rawPassword 암호화할 평문 비밀번호 (사용자가 입력한 원본)
	 * @return 암호화된 비밀번호 (BCrypt 해시값, Salt 포함, 약 60자)
	 * @throws IllegalArgumentException 비밀번호가 null이거나 빈 문자열인 경우
	 */
	public String encodePassword(String rawPassword) {
		// 1. 입력값 검증 (null 또는 빈 문자열 체크)
		if (rawPassword == null || rawPassword.trim().isEmpty()) {
			throw new IllegalArgumentException("비밀번호는 null이거나 빈 값일 수 없습니다.");
		}

		// 2. BCrypt 암호화 수행
		// - passwordEncoder.encode() 호출 시마다 다른 Salt 생성
		// - 내부적으로 SecureRandom을 사용하여 암호학적으로 안전한 Salt 생성
		String encodedPassword = passwordEncoder.encode(rawPassword);

		// 3. 암호화 성공 로그 (실제 비밀번호 값은 출력하지 않음)
		log.debug("Password encoded successfully");

		// 4. 암호화된 비밀번호 반환
		return encodedPassword;
	}

	/**
	 * 평문 비밀번호와 암호화된 비밀번호 일치 여부 확인
	 * - 로그인, 비밀번호 변경 시 기존 비밀번호 검증에 사용
	 * - BCrypt는 같은 비밀번호도 매번 다른 해시값을 생성하므로
	 *   직접 문자열 비교(equals)가 아닌 matches() 메서드 사용 필수
	 *
	 * BCrypt 비교 과정:
	 * 1. 암호화된 비밀번호에서 Salt 추출
	 * 2. 평문 비밀번호와 추출한 Salt를 결합
	 * 3. BCrypt 해싱 알고리즘 적용
	 * 4. 생성된 해시값과 저장된 해시값 비교
	 * 5. 일치 여부 반환
	 *
	 * 사용 예시:
	 * - AdminAuthService.login(): 관리자 로그인 시 비밀번호 검증
	 * - AuthService.login(): 사용자 로그인 시 비밀번호 검증
	 * - AdminService.updatePassword(): 비밀번호 변경 시 기존 비밀번호 확인
	 *
	 * 보안 고려사항:
	 * - Timing Attack 방지를 위해 일정한 시간 소요
	 * - 비밀번호가 틀려도 즉시 반환하지 않고 전체 검증 수행
	 *
	 * @param rawPassword 사용자가 입력한 평문 비밀번호
	 * @param encodedPassword 데이터베이스에 저장된 암호화된 비밀번호
	 * @return 일치하면 true, 불일치하거나 파라미터가 null이면 false
	 */
	public boolean matches(String rawPassword, String encodedPassword) {
		// 1. null 체크 (null 안전 처리)
		if (rawPassword == null || encodedPassword == null) {
			return false;
		}

		// 2. BCrypt 비교 수행
		// - passwordEncoder.matches()는 내부적으로:
		//   (1) encodedPassword에서 Salt 추출
		//   (2) rawPassword와 Salt로 새로운 해시 생성
		//   (3) 생성된 해시와 저장된 해시 비교
		boolean isMatch = passwordEncoder.matches(rawPassword, encodedPassword);

		// 3. 비교 결과 로그 (실제 비밀번호 값은 출력하지 않음)
		log.debug("Password match result: {}", isMatch);

		// 4. 일치 여부 반환
		return isMatch;
	}

	/**
	 * 비밀번호 강도 검증
	 * - 안전한 비밀번호 정책 강제
	 * - 회원가입, 비밀번호 변경 시 검증에 사용
	 * - 비밀번호 복잡도 요구사항 충족 여부 확인
	 *
	 * 비밀번호 강도 요구사항:
	 * 1. 최소 길이: 8자 이상
	 * 2. 영문자 포함: 대문자(A-Z) 또는 소문자(a-z) 최소 1개
	 * 3. 숫자 포함: 0-9 최소 1개
	 * 4. 특수문자 포함: @$!%*?& 중 최소 1개
	 *
	 * 검증 로직:
	 * - Stream API의 anyMatch()를 사용하여 조건 충족 여부 확인
	 * - 모든 조건을 AND 연산으로 결합 (모두 만족해야 true)
	 *
	 * 사용 예시:
	 * - AdminService.updatePassword(): 새 비밀번호 강도 검증
	 * - UserService.updatePassword(): 새 비밀번호 강도 검증
	 * - SignupRequestDto: @Pattern 어노테이션 대신 서비스 레벨 검증 가능
	 *
	 *
	 * @param password 검증할 비밀번호 문자열
	 * @return 모든 강도 요구사항을 충족하면 true, 그렇지 않으면 false
	 */
	public boolean isStrongPassword(String password) {
		// 1. 기본 검증: null 체크 및 최소 길이 확인 (8자 이상)
		if (password == null || password.length() < 8) {
			return false;
		}

		// 2. 영문자 포함 여부 확인 (대문자 또는 소문자)
		// - chars(): 문자열을 IntStream으로 변환
		// - anyMatch(): 조건을 만족하는 문자가 하나라도 있는지 확인
		// - (c >= 'a' && c <= 'z'): 소문자 a-z 범위
		// - (c >= 'A' && c <= 'Z'): 대문자 A-Z 범위
		boolean hasLetter = password.chars().anyMatch(c ->
			(c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));

		// 3. 숫자 포함 여부 확인 (0-9)
		// - Character.isDigit(): 숫자인지 확인하는 유틸리티 메서드
		boolean hasDigit = password.chars().anyMatch(Character::isDigit);

		// 4. 특수문자 포함 여부 확인 (@$!%*?& 중 하나)
		// - indexOf(): 특수문자 목록에 해당 문자가 있는지 확인
		// - indexOf() >= 0: 문자가 존재하면 0 이상의 인덱스 반환
		boolean hasSpecialChar = password.chars().anyMatch(c ->
			"@$!%*?&".indexOf(c) >= 0);

		// 5. 모든 조건을 AND 연산으로 결합
		// - hasLetter && hasDigit && hasSpecialChar
		// - 세 가지 조건 모두 true여야 true 반환
		return hasLetter && hasDigit && hasSpecialChar;
	}
}
