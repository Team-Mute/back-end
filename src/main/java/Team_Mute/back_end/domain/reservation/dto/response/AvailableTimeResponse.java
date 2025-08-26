package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.LocalTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AvailableTimeResponse {
	private List<TimeSlot> availableTimes;

	@Getter
	@AllArgsConstructor
	public static class TimeSlot {
		private LocalTime startTime;
		private LocalTime endTime;
	}
}
