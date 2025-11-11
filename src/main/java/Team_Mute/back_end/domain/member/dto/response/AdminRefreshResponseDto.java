package Team_Mute.back_end.domain.member.dto.response;

import lombok.Getter;

/**
 * 관리자 토큰 재발급 응답 DTO
 * JWT Refresh Token을 통한 Access Token 재발급 시 클라이언트에 반환되는 데이터
 * AdminAuthController의 refresh API 응답으로 사용
 * RTR(Refresh Token Rotation) 전략에 따라 새로운 Access Token 전달
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
public class AdminRefreshResponseDto {
	/**
	 * 재발급된 JWT Access Token
	 * - API 요청 시 Authorization 헤더에 포함하여 사용
	 * - 기존 Access Token이 만료되었을 때 새로 발급
	 * - 새로운 Refresh Token은 HttpOnly 쿠키로 별도 전송
	 */
	private String accessToken;

	/**
	 * AdminRefreshResponseDto 생성자
	 *
	 * @param accessToken 재발급된 JWT Access Token
	 */
	public AdminRefreshResponseDto(String accessToken) {
		this.accessToken = accessToken;
	}
}
