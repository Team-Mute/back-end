package Team_Mute.back_end.domain.smsAuth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerificationRequestDto {
	@NotBlank(message = "전화번호를 입력해주세요.")
	@Pattern(regexp = "^[0-9]{11}$", message = "전화번호는 숫자만 입력 가능합니다.")
	private String phoneNumber;

	@NotBlank(message = "인증번호를 입력해주세요.")
	@Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자입니다.")
	private String verificationCode;
}
