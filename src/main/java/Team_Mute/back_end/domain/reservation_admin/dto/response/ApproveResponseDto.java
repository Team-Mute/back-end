package Team_Mute.back_end.domain.reservation_admin.dto.response;

import java.time.LocalDateTime;

/**
 * [단일 예약 승인] 처리 결과 응답 DTO
 * * 예약이 성공적으로 1차 또는 2차 승인되었을 때, 변경 전/후 상태와 처리 일시를 반환
 */
public class ApproveResponseDto {
	/**
	 * 승인 처리된 예약 ID
	 */
	public Long reservationId;

	/**
	 * 승인 처리 전 예약 상태 이름 (From Status)
	 */
	public String fromStatus;

	/**
	 * 승인 처리 후 예약 상태 이름 (To Status)
	 */
	public String toStatus;

	/**
	 * 승인 처리 완료 시각
	 */
	public LocalDateTime approvedAt;

	/**
	 * 처리 결과 메시지 (예: "1차 승인 완료", "2차 승인 완료")
	 */
	public String message;

	/**
	 * 모든 필드를 포함하는 생성자
	 */
	public ApproveResponseDto(Long reservationId, String fromStatus, String toStatus, LocalDateTime approvedAt, String message) {
		this.reservationId = reservationId;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.approvedAt = approvedAt;
		this.message = message;
	}

	/**
	 * 처리 결과 메시지를 반환
	 */
	public String getMessage() {
		return this.message;
	}
}
