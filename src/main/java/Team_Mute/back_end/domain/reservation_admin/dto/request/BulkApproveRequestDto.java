package Team_Mute.back_end.domain.reservation_admin.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * [예약 일괄 승인] 요청 DTO
 * * 관리자가 다수의 예약을 한 번에 승인하기 위해 요청하는 예약 ID 목록을 담고 있음
 */
public class BulkApproveRequestDto {
	/**
	 * 일괄 승인할 예약 ID(reservationId) 목록
	 * * {@code @NotEmpty}: 목록이 비어 있으면 안 됨
	 */
	@NotEmpty(message = "승인할 예약 ID 목록은 필수입니다.")
	private List<Long> reservationIds;

	/**
	 * 기본 생성자 (JSON 역직렬화를 위해 필요)
	 */
	public BulkApproveRequestDto() {
	}

	/**
	 * 필드 값을 모두 받는 생성자 (JSON 역직렬화를 위해 사용될 수 있음)
	 *
	 * @param reservationIds 일괄 승인할 예약 ID 목록
	 */
	public BulkApproveRequestDto(List<Long> reservationIds) {
		this.reservationIds = reservationIds;
	}

	/**
	 * 예약 ID 목록을 반환
	 *
	 * @return 예약 ID 목록
	 */
	public List<Long> getReservationIds() {
		return reservationIds;
	}
}
