package Team_Mute.back_end.domain.reservation_admin.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationFilterOptionsResponse {

	private List<StatusOptionDto> statuses;
	private List<FlagOptionDto> flags;

	// 상태 옵션 DTO
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class StatusOptionDto {
		private Integer id;
		private String label;
	}

	// Flag 옵션 DTO
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class FlagOptionDto {
		private String key;
		private String label;
	}
}
