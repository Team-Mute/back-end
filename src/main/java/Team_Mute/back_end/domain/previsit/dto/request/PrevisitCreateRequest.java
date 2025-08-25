package Team_Mute.back_end.domain.previsit.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PrevisitCreateRequest {

	@NotNull(message = "예약 ID는 필수입니다.")
	private Long reservationId;

	@NotNull(message = "사전답사 시작 시간은 필수입니다.")
	@Future(message = "사전답사 시작 시간은 현재 시간 이후여야 합니다.")
	private LocalDateTime previsitFrom;

	@NotNull(message = "사전답사 종료 시간은 필수입니다.")
	@Future(message = "사전답사 종료 시간은 현재 시간 이후여야 합니다.")
	private LocalDateTime previsitTo;
}
