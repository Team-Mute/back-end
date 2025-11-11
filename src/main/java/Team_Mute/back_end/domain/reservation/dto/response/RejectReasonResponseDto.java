package Team_Mute.back_end.domain.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 반려 사유 조회 응답 DTO
 * 관리자가 예약을 반려한 경우 반려 사유 메시지 반환
 *
 * 사용 목적:
 * - 사용자에게 예약 반려 이유 전달
 * - 재예약 시 참고 자료 제공
 *
 * API 엔드포인트:
 * - GET /api/reservations/rejectMassage/{reservation_id}
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectReasonResponseDto {
	/**
	 * 반려 사유 메시지
	 * - 관리자가 작성한 반려 사유
	 * - Reservation 엔티티의 memo 필드 값
	 */
	private String memo;
}
