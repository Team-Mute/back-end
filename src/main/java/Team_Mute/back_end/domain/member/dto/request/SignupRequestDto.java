package Team_Mute.back_end.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "회원가입 req DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDto {

	@Schema(description = "사용자 이름", example = "김뮤트")
	@NotBlank(message = "사용자 이름은 필수 입력 항목입니다.")
	@Size(min = 2, max = 50, message = "사용자 이름은 2자 이상 50자 이하로 입력해주세요.")
	@Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "사용자 이름은 한글, 영문만 입력 가능합니다.")
	private String userName;

	@Schema(description = "사용자 이메일", example = "mute@example.com")
	@NotBlank(message = "이메일은 필수 입력 항목입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
	private String userEmail;

	@Schema(description = "사용자 비밀번호", example = "example123!")
	@NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
	@Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
	@Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[a-zA-Z\\d@$!%*?&]+$",
		message = "비밀번호는 영문, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다.")
	private String userPwd;

	@Schema(description = "소속 기업명", example = "(주)뮤트")
	@NotBlank(message = "회사명은 필수 입력 항목입니다.")
	@Size(min = 1, max = 50, message = "회사명은 1자 이상 50자 이하여야 합니다.")
	private String companyName;

	@Schema(description = "이메일 수신 동의", example = "true")
	@NotNull(message = "이메일 알림 동의 여부를 선택해주세요.")
	private Boolean agreeEmail;
}
