package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PasswordUpdateRequestDto {
	@NotBlank(message = "현재 비밀번호를 입력해주세요.")
	private String password;

	@NotBlank(message = "새로운 비밀번호를 입력해주세요.")
	@Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
	private String newPassword;
}
