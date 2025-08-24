package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AdminSignupRequestDto {
	@NotNull(message = "역할 ID는 필수 입력 값입니다.")
	@Min(value = 0, message = "역할 ID는 0 또는 1 또는 2여야 합니다.")
	@Max(value = 2, message = "역할 ID는 0 또는 1 또는 2여야 합니다.")
	private Integer roleId;

	private String regionName;

	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String userEmail;

	@NotBlank(message = "이름은 필수 입력 값입니다.")
	private String userName;

	@NotBlank(message = "전화번호는 필수 입력 값입니다.")
	private String userPhone;
}
