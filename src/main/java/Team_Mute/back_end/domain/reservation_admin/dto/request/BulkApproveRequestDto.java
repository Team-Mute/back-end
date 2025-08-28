package Team_Mute.back_end.domain.reservation_admin.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BulkApproveRequestDto {
	@NotEmpty
	private List<Long> reservationIds;

	public BulkApproveRequestDto() {
	}

	public BulkApproveRequestDto(List<Long> reservationIds) {
		this.reservationIds = reservationIds;
	}

	public List<Long> getReservationIds() {
		return reservationIds;
	}

	public void setReservationIds(List<Long> reservationIds) {
		this.reservationIds = reservationIds;
	}
}
