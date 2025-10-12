package Team_Mute.back_end.domain.reservation_admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * [예약 관리 -> 예약 목록 필터 옵션] 응답 DTO
 * * 예약 목록 검색/필터링을 위한 드롭다운 옵션 (상태, 플래그 등) 목록을 클라이언트에게 전달
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationFilterOptionsResponse {

	/**
	 * 예약 상태 필터 옵션 리스트 (예: "1차 승인 대기", "2차 승인 대기" 등)
	 */
	private List<StatusOptionDto> statuses;

	/**
	 * 예약 플래그 필터 옵션 리스트 (예: 긴급, 신한)
	 */
	private List<FlagOptionDto> flags;

	/**
	 * [상태 옵션] 내부 DTO
	 * * 상태 필터 드롭다운의 각 항목을 나타냅니다.
	 */
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class StatusOptionDto {
		/**
		 * 예약 상태 ID (필터링에 사용될 값, 예: 1, 2 등)
		 */
		private Integer id;

		/**
		 * 상태 표시 이름 (클라이언트에게 보여줄 이름, 예: "1차 승인 대기", "2차 승인 대기" 등)
		 */
		private String label;
	}

	/**
	 * [Flag 옵션] 내부 DTO
	 * * 플래그 필터 드롭다운의 각 항목을 나타냅니다.
	 */
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class FlagOptionDto {
		/**
		 * 플래그 키 (필터링에 사용될 값, 예: "isShinhan", "isEmergency")
		 */
		private String key;

		/**
		 * 플래그 표시 이름 (클라이언트에게 보여줄 이름, 예: "신한 예약", "긴급 예약")
		 */
		private String label;
	}
}
