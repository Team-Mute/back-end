package Team_Mute.back_end.domain.reservation_admin.dto.response;

/**
 * [예약 일괄 승인] 목록 내 개별 항목 처리 결과 DTO
 * * {@code BulkApproveResponseDto} 내의 {@code results} 리스트에 포함
 */
public class BulkApproveItemResultDto {
	/**
	 * 처리된 예약 ID
	 */
	private Long reservationId;

	/**
	 * 해당 예약 건의 처리 성공 여부
	 */
	private boolean success;

	/**
	 * 해당 예약 건에 대한 처리 메시지 (성공/실패 사유)
	 */
	private String message;

	/**
	 * 기본 생성자 (JSON 직렬화를 위해 필요)
	 */
	public BulkApproveItemResultDto() {
	}

	/**
	 * 모든 필드를 포함하는 생성자
	 */
	public BulkApproveItemResultDto(Long reservationId, boolean success, String message) {

		this.reservationId = reservationId;
		this.success = success;
		this.message = message;
	}

	/**
	 * 처리된 예약 ID를 반환
	 */
	public Long getReservationId() {
		return reservationId;
	}

	/**
	 * 처리 성공 여부를 반환
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * 처리 결과 메시지(성공/실패 사유)를 반환
	 */
	public String getMessage() {
		return message;
	}
}
