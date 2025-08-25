package Team_Mute.back_end.domain.reservation_admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RejectResponseDto {
	private Long reservationId;
	private String fromStatus;
	private String toStatus;
	private LocalDateTime rejectedAt;
	private String rejectionReason;
	private String message;
}
