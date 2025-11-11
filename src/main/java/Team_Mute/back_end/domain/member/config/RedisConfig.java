package Team_Mute.back_end.domain.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 설정 관련 클래스
 * Redis와의 상호작용을 위한 StringRedisTemplate 빈을 등록하여 애플리케이션 전역에서 Redis 데이터 저장 및 조회 기능 제공
 * 주로 세션 관리, JWT Refresh Token 저장, 이메일/SMS 인증 코드 임시 저장 등에 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Configuration
public class RedisConfig {

	/**
	 * StringRedisTemplate 빈 등록
	 * - Redis의 Key와 Value를 모두 String 타입으로 처리하는 템플릿 클래스
	 * - RedisTemplate<String, String>을 상속하며, StringRedisSerializer를 기본으로 사용하여 직렬화/역직렬화 자동 처리
	 * - 세션 정보, Refresh Token, 인증 코드 등 문자열 기반 임시 데이터 저장에 최적화
	 *
	 * @param cf Spring Boot가 자동으로 생성한 RedisConnectionFactory (application.properties의 Redis 연결 정보 기반)
	 * @return StringRedisTemplate 인스턴스 (Redis 데이터 저장 및 조회에 사용)
	 */
	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
		return new StringRedisTemplate(cf);
	}
}
