package Team_Mute.back_end.domain.previsit.dto.response;

import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PrevisitResponse {
	private Long previsitId;
	private Long reservationId;
	private LocalDateTime previsitFrom;
	private LocalDateTime previsitTo;
	private LocalDateTime regDate;
	private LocalDateTime updDate;

	public static PrevisitResponse from(PrevisitReservation entity) {
		return new PrevisitResponse(
			entity.getId(),
			entity.getReservation().getReservationId(),
			entity.getPrevisitFrom(),
			entity.getPrevisitTo(),
			entity.getRegDate(),
			entity.getUpdDate()
		);
	}
}
