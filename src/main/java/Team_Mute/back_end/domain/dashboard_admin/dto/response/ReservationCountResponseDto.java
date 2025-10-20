package Team_Mute.back_end.domain.dashboard_admin.dto.response;

import lombok.Getter;

/**
 * 관리자 대시보드에서 예약 현황 카운트를 반환하는 DTO
 * 1차 승인 대기, 2차 승인 대기, 긴급, 신한 관련 예약 건수를 포함
 * Lombok의 {@code @Getter}를 사용하여 필드에 대한 Getter 메서드를 자동 생성
 */
@Getter
public class ReservationCountResponseDto {
	private long waitingFistApprovalCount;
	private long waitingSecondApprovalCount;
	private long emergency;
	private long shinhan;

	/**
	 * 예약 건수 응답 DTO의 생성자
	 *
	 * @param waitingFistApprovalCount   1차 승인 대기 건수
	 * @param waitingSecondApprovalCount 2차 승인 대기 건수
	 * @param emergencyCount             긴급 예약 건수
	 * @param shinhanCount               신한 관련 예약 건수
	 */
	public ReservationCountResponseDto(long waitingFistApprovalCount, long waitingSecondApprovalCount, long emergencyCount, long shinhanCount) {
		this.waitingFistApprovalCount = waitingFistApprovalCount;
		this.waitingSecondApprovalCount = waitingSecondApprovalCount;
		this.emergency = emergencyCount;
		this.shinhan = shinhanCount;
	}
}
