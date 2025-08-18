package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AdminLoginDto {
	public record Request(@NotBlank String email, @NotBlank String password) {
	}

	public record Response(String accessToken) {
	}
}
