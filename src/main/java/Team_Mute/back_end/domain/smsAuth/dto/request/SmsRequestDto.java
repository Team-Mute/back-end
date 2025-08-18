package Team_Mute.back_end.domain.smsAuth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SmsRequestDto {
	@NotBlank(message = "국가 코드를 입력해주세요.")
	@Pattern(regexp = "^[0-9]+$", message = "국가 코드는 숫자만 입력 가능합니다.")
	private String countryCode;

	@NotBlank(message = "전화번호를 입력해주세요.")
	@Pattern(regexp = "^[0-9]{11}$", message = "전화번호는 11자리 숫자만 입력 가능합니다. ('-' 제외)")
	private String phoneNumber;
}
