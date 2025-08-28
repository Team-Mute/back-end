package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.ZonedDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservationCancelResponseDto {

	private Long reservationId;
	private String fromStatus;
	private String toStatus;
	private ZonedDateTime approvedAt;
	private String message;
}
