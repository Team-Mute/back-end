package Team_Mute.back_end.domain.member.jwt;

/**
 * Access Token과 Refresh Token 쌍을 담는 불변 데이터 클래스 (Java Record)
 * 로그인 또는 토큰 재발급 시 두 개의 토큰을 함께 반환하기 위한 래퍼 클래스
 * AuthService와 AdminAuthService에서 로그인/재발급 메서드의 반환 타입으로 사용
 * 불변 객체로 스레드 안전성 보장
 *
 * 사용 흐름:
 * 1. 로그인 시: AuthService.login() → TokenPair 반환
 * 2. 토큰 재발급 시: AuthService.refresh() → TokenPair 반환
 * 3. 컨트롤러에서: Access Token은 응답 바디, Refresh Token은 HttpOnly 쿠키로 전달
 *
 * @author Team Mute
 * @since 1.0
 */
public record TokenPair(
	/**
	 * JWT Access Token
	 * - API 요청 시 인증에 사용되는 단기 유효 토큰
	 * - 클라이언트에서 Authorization 헤더에 "Bearer {accessToken}" 형식으로 포함
	 * - 짧은 만료 시간 사용
	 * - 만료 시 Refresh Token으로 재발급
	 * - 응답 바디(JSON)로 클라이언트에 전달
	 */
	String accessToken,

	/**
	 * JWT Refresh Token
	 * - Access Token 재발급에 사용되는 장기 유효 토큰
	 * - HttpOnly 쿠키로 저장하여 JavaScript 접근 차단 (XSS 공격 방지)
	 * - 긴 만료 시간 사용
	 * - RTR(Refresh Token Rotation) 전략으로 매 재발급 시 새로운 토큰 생성
	 * - Redis에 저장하여 서버 측에서 무효화 관리 가능
	 * - 로그아웃 시 Redis에서 삭제하여 즉시 무효화
	 */
	String refreshToken
) {
}
