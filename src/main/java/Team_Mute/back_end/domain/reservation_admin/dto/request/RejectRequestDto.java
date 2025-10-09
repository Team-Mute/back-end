package Team_Mute.back_end.domain.reservation_admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [예약 반려] 요청 DTO
 * * 관리자가 예약을 반려할 때, 그 사유(rejectionReason)를 담아 전송
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectRequestDto {
	/**
	 * 예약 반려 사유
	 * * {@code @NotBlank}: 공백이거나 비어 있으면 안 됩니다.
	 */
	@NotBlank(message = "반려 사유는 필수입니다.")
	private String rejectionReason;
}
