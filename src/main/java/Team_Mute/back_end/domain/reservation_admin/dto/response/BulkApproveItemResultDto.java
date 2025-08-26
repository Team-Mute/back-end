package Team_Mute.back_end.domain.reservation_admin.dto.response;

public class BulkApproveItemResultDto {
	private Long reservationId;
	private boolean success;
	private String message;

	public BulkApproveItemResultDto() {
	}

	public BulkApproveItemResultDto(Long reservationId, boolean success, String message) {
		this.reservationId = reservationId;
		this.success = success;
		this.message = message;
	}

	public Long getReservationId() {
		return reservationId;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}
}
