package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AdminPasswordResetRequest {
	@NotBlank(message = "초기화할 회원의 이메일은 필수입니다.")
	@Email
	private String userEmail;
}
