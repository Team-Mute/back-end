package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AdminPasswordUpdateRequest {
	@NotBlank(message = "현재 비밀번호는 필수 입력 값입니다.")
	private String password;

	@NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
	@Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
	private String newPassword;
}
