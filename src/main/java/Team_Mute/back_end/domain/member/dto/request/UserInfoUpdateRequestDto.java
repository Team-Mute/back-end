package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UserInfoUpdateRequestDto {
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String userEmail;

	@NotBlank(message = "이름은 필수 입력 값입니다.")
	private String userName;

	@NotNull(message = "이메일 수신 동의 여부를 입력해주세요.")
	private Boolean agreeEmail;
}
