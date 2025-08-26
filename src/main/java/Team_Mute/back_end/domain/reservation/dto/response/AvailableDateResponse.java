package Team_Mute.back_end.domain.reservation.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AvailableDateResponse {
	private List<Integer> availableDays;
}
