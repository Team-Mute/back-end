package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.LocalDateTime;

import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrevisitInfo {
	private Long previsitId;
	private LocalDateTime previsitFrom;
	private LocalDateTime previsitTo;

	public static PrevisitInfo fromEntity(PrevisitReservation previsit) {
		if (previsit == null) {
			return null;
		}
		return PrevisitInfo.builder()
			.previsitId(previsit.getPrevisitId())
			.previsitFrom(previsit.getPrevisitFrom())
			.previsitTo(previsit.getPrevisitTo())
			.build();
	}
}
