package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AdminLoginDto {
	public record Request(@NotBlank String email, @NotBlank String password) {
	}

	public static class Response {
		private String accessToken;
		private Integer roleId; // roleId 필드 추가

		// 생성자 수정
		public Response(String accessToken, Integer roleId) {
			this.accessToken = accessToken;
			this.roleId = roleId;
		}

		// Getter
		public String getAccessToken() {
			return accessToken;
		}

		public Integer getRoleId() {
			return roleId;
		}
	}
}
