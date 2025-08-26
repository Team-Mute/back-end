package Team_Mute.back_end.domain.reservation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailableDateRequest {
	@NotNull(message = "공간 ID는 필수입니다.")
	private Integer spaceId;

	@NotNull(message = "년도는 필수입니다.")
	@Min(2000)
	private Integer year;

	@NotNull(message = "월은 필수입니다.")
	@Min(1)
	@Max(12)
	private Integer month;
}
