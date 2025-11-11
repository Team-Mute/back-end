package Team_Mute.back_end.domain.member.dto.response;

/**
 * 토큰 재발급 성공 시 반환되는 응답 DTO
 * - 새로 발급된 Access Token을 클라이언트에 전달
 * - 새로운 Refresh Token은 HttpOnly 쿠키로 별도 전송
 *
 * @param accessToken 재발급된 JWT Access Token
 *
 * @author Team Mute
 * @since 1.0
 */
public record RefreshResponse(String accessToken) {
}
