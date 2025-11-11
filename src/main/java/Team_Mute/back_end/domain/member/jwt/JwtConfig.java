package Team_Mute.back_end.domain.member.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정을 담는 불변 설정 클래스 (Java Record)
 * application.properties 파일의 jwt.* 속성을 자동으로 바인딩
 * ConfigRegistrar에서 @EnableConfigurationProperties로 활성화
 * 불변 객체로 스레드 안전성 보장
 *
 * @author Team Mute
 * @since 1.0
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
	/**
	 * JWT 발급자(issuer) 식별자
	 * - JWT 표준 클레임 "iss"에 해당
	 * - 토큰을 발급한 서버/애플리케이션 식별
	 * - 토큰 검증 시 이 값과 일치하는지 확인
	 */
	String issuer,

	/**
	 * JWT 대상(audience) 식별자
	 * - JWT 표준 클레임 "aud"에 해당
	 * - 토큰이 의도된 수신자(클라이언트) 식별
	 * - 토큰이 올바른 대상에게 사용되는지 검증
	 */
	String audience,

	/**
	 * Access Token 설정
	 * - TTL(Time To Live) 정보 포함
	 * - 중첩 Record로 구조화된 설정
	 */
	Access accessToken,

	/**
	 * Refresh Token 설정
	 * - TTL(Time To Live) 정보 포함
	 * - 중첩 Record로 구조화된 설정
	 */
	Refresh refreshToken,

	/**
	 * JWT 서명에 사용할 비밀 키 (Base64 인코딩)
	 * - HMAC-SHA256 알고리즘에 사용되는 대칭 키
	 * - 최소 256비트(32바이트) 권장
	 * - Base64로 인코딩하여 application.properties에 저장
	 * - 절대 외부에 노출되어서는 안 됨 (환경 변수 사용 권장)
	 * - 키 생성 예: openssl rand -base64 32
	 */
	String secretBase64
) {
	/**
	 * Access Token 설정을 담는 중첩 Record
	 *
	 * @param ttlSeconds Access Token 만료 시간 (초 단위)
	 *                   - 짧은 만료 시간으로 보안 강화
	 *                   - API 요청 시 Authorization 헤더에 포함하여 사용
	 *                   - 만료 시 Refresh Token으로 재발급
	 */
	public record Access(long ttlSeconds) {
	}

	/**
	 * Refresh Token 설정을 담는 중첩 Record
	 *
	 * @param ttlSeconds Refresh Token 만료 시간 (초 단위)
	 *                   - 긴 만료 시간 설정
	 *                   - Access Token 재발급에 사용
	 *                   - HttpOnly 쿠키로 저장하여 XSS 공격 방지
	 *                   - RTR 전략으로 매 재발급 시 새로운 Refresh Token 생성
	 */
	public record Refresh(long ttlSeconds) {
	}
}
