package Team_Mute.back_end.domain.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 비밀번호 암호화 관련 설정을 담당하는 클래스
 * BCryptPasswordEncoder 빈을 등록하여 애플리케이션 전역에서 비밀번호 암호화 및 검증 기능 제공
 * 회원가입 시 비밀번호 해시 저장, 로그인 시 비밀번호 일치 여부 검증에 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Configuration
public class PasswordConfig {

	/**
	 * BCryptPasswordEncoder 빈 등록
	 * - Spring Security에서 제공하는 BCrypt 해싱 알고리즘 기반 비밀번호 암호화 도구
	 * - 랜덤 Salt를 추가하여 같은 비밀번호도 매번 다른 해시값을 생성하므로 보안성 강화
	 * - encode() 메서드로 평문 비밀번호를 암호화하여 DB에 저장
	 * - matches() 메서드로 로그인 시 입력된 비밀번호와 저장된 해시값 일치 여부 검증
	 * - 무차별 대입 공격(Brute Force Attack)에 강한 단방향 해시 알고리즘 사용
	 *
	 * @return BCryptPasswordEncoder 인스턴스 (비밀번호 암호화 및 검증에 사용)
	 */
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
