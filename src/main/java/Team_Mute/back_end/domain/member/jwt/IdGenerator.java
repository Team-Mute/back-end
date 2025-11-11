package Team_Mute.back_end.domain.member.jwt;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * JWT 관련 고유 식별자 생성 유틸리티 클래스
 * 세션 ID, Access Token JTI, Refresh Token JTI를 생성하는 정적 메서드 제공
 * SecureRandom과 UUID를 사용하여 암호학적으로 안전한 고유 식별자 생성
 * final 클래스로 선언하여 상속 방지 (유틸리티 클래스 패턴)
 *
 * @author Team Mute
 * @since 1.0
 */
public final class IdGenerator {
	/**
	 * 암호학적으로 안전한 난수 생성기
	 * - SecureRandom은 예측 불가능한 난수 생성
	 * - 멀티스레드 환경에서 안전하게 사용 가능
	 * - static으로 선언하여 클래스 로딩 시 한 번만 초기화
	 */
	private static final SecureRandom RNG = new SecureRandom();

	/**
	 * 새로운 세션 ID 생성
	 * - Redis에 저장되는 세션의 고유 식별자
	 * - "sess-" 접두사 + UUID v4 형식
	 * - 예: "sess-123e4567-e89b-12d3-a456-426614174000"
	 * - 로그인 시 세션 추적 및 관리에 사용
	 *
	 * @return 생성된 세션 ID 문자열
	 */
	public static String newSid() {
		return "sess-" + UUID.randomUUID();
	}

	/**
	 * 새로운 Access Token JTI(JWT ID) 생성
	 * - JWT 표준 클레임인 jti 값으로 사용
	 * - "at-" 접두사 + UUID v4 형식
	 * - 예: "at-123e4567-e89b-12d3-a456-426614174000"
	 * - 토큰 고유 식별 및 블랙리스트 관리에 사용
	 * - 로그아웃 시 해당 jti를 Redis 블랙리스트에 등록하여 토큰 무효화
	 *
	 * @return 생성된 Access Token JTI 문자열
	 */
	public static String newJtiAT() {
		return "at-" + UUID.randomUUID();
	}

	/**
	 * 새로운 Refresh Token JTI 생성
	 * - RTR(Refresh Token Rotation) 전략을 위한 고유 식별자
	 * - "rt-" 접두사 + 256비트 무작위 바이트를 Base64 URL 인코딩
	 * - 예: "rt-xYz123ABC..."
	 * - UUID 대신 SecureRandom을 사용하여 더 높은 엔트로피 제공
	 * - Base64 URL 인코딩으로 URL 안전 문자열 생성 (패딩 제거)
	 * - Redis에 Refresh Token 저장 시 키로 사용
	 *
	 * @return 생성된 Refresh Token JTI 문자열
	 */
	public static String newJtiRT() {
		byte[] buf = new byte[32]; // 256bit
		RNG.nextBytes(buf);
		return "rt-" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}
}
