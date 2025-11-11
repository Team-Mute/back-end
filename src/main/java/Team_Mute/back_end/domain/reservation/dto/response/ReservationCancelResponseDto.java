package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.ZonedDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * 예약 취소 응답 DTO
 * 예약 취소 처리 결과를 클라이언트에 전달
 *
 * 사용 목적:
 * - 예약 취소 성공 확인
 * - 상태 변경 이력 기록 (fromStatus → toStatus)
 * - 사용자에게 취소 완료 메시지 전달
 *
 * API 엔드포인트:
 * - POST /api/reservations/cancel/{reservation_id}
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@Builder
public class ReservationCancelResponseDto {

	/**
	 * 예약 ID
	 * - 취소된 예약의 고유 식별자
	 * - Reservation 엔티티의 reservationId
	 */
	private Long reservationId;

	/**
	 * 이전 상태
	 * - 취소 전 예약 상태
	 * - 상태 이름 (예: "예약완료", "진행중")
	 * - 이력 추적 목적
	 */
	private String fromStatus;

	/**
	 * 변경된 상태
	 * - 취소 후 예약 상태
	 */
	private String toStatus;

	/**
	 * 취소 일시
	 * - 취소된 일시
	 * - 취소 이력 추적 목적
	 */
	private ZonedDateTime approvedAt;

	/**
	 * 완료 메시지
	 * - 취소 성공 확인 및 안내
	 */
	private String message;
}
