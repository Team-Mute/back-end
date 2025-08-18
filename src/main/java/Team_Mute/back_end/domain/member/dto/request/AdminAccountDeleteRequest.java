package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AdminAccountDeleteRequest {
	@NotBlank(message = "삭제할 회원의 이메일은 필수입니다.")
	@Email
	private String userEmail;
}
