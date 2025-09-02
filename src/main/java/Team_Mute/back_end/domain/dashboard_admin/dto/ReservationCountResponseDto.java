package Team_Mute.back_end.domain.dashboard_admin.dto;

import lombok.Getter;

@Getter
public class ReservationCountResponseDto {
	private long waitingFistApprovalCount;
	private long waitingSecondApprovalCount;
	private long emergency;
	private long shinhan;

	public ReservationCountResponseDto(long waitingFistApprovalCount, long waitingSecondApprovalCount, long emergencyCount, long shinhanCount) {
		this.waitingFistApprovalCount = waitingFistApprovalCount;
		this.waitingSecondApprovalCount = waitingSecondApprovalCount;
		this.emergency = emergencyCount;
		this.shinhan = shinhanCount;
	}
}
