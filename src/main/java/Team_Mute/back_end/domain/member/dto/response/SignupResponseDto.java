package Team_Mute.back_end.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponseDto {
	private String message;
	private Long userId;

	public SignupResponseDto(String message) {
		this.message = message;
	}
}
