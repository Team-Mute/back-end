package Team_Mute.back_end.domain.reservation_admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * [예약 반려] 처리 결과 응답 DTO
 * * 예약이 성공적으로 반려되었을 때, 변경 전/후 상태, 반려 일시 및 사유를 반환
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RejectResponseDto {
	/**
	 * 반려 처리된 예약 ID
	 */
	private Long reservationId;

	/**
	 * 반려 처리 전 예약 상태 이름 (From Status)
	 */
	private String fromStatus;

	/**
	 * 반려 처리 후 예약 상태 이름 (To Status - 항상 '반려' 상태)
	 */
	private String toStatus;

	/**
	 * 반려 처리 완료 시각
	 */
	private LocalDateTime rejectedAt;

	/**
	 * 기록된 반려 사유
	 */
	private String rejectionReason;

	/**
	 * 처리 결과 메시지 (예: "반려 완료")
	 */
	private String message;
}
