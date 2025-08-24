package Team_Mute.back_end.domain.member.dto.response;

import lombok.Getter;

@Getter
public class AdminRefreshResponseDto {
	private String accessToken;

	public AdminRefreshResponseDto(String accessToken) {
		this.accessToken = accessToken;
	}
}
