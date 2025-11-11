package Team_Mute.back_end.domain.member.dto.response;

/**
 * 로그인 성공 시 반환되는 응답 DTO
 * - Access Token을 클라이언트에 전달하여 API 요청 시 Authorization 헤더에 포함하도록 함
 * - Refresh Token은 HttpOnly 쿠키로 별도 전송
 *
 * @param accessToken JWT Access Token (API 인증에 사용)
 *
 * @author Team Mute
 * @since 1.0
 */
public record TokenResponse(String accessToken) {
}
