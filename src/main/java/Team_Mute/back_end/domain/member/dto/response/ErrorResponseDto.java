package Team_Mute.back_end.domain.member.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
	private String message;
	private int status;
	private LocalDateTime timestamp;

	public ErrorResponseDto(String message, int status) {
		this.message = message;
		this.status = status;
		this.timestamp = LocalDateTime.now();
	}
}
