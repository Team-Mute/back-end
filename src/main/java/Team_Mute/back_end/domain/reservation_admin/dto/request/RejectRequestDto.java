package Team_Mute.back_end.domain.reservation_admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectRequestDto {
	@NotBlank(message = "반려 사유는 필수입니다.")
	private String rejectionReason;
}
