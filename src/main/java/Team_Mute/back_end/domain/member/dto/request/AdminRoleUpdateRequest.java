package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AdminRoleUpdateRequest {
	@NotBlank(message = "수정할 회원의 이메일은 필수입니다.")
	@Email
	private String userEmail;

	@NotNull(message = "역할 ID는 필수 입력 값입니다.")
	@Min(value = 1, message = "역할 ID는 1 또는 2여야 합니다.")
	@Max(value = 2, message = "역할 ID는 1 또는 2여야 합니다.")
	private Integer roleId;
}
