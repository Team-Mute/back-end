package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AdminAccountUpdateRequest {
	@NotBlank(message = "지역 이름은 필수 입력 값입니다.")
	private String regionName;

	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String userEmail;

	@NotBlank(message = "이름은 필수 입력 값입니다.")
	private String userName;

	@NotBlank(message = "전화번호는 필수 입력 값입니다.")
	private String userPhone;
}
