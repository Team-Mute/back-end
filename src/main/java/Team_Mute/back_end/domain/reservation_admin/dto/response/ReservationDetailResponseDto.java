package Team_Mute.back_end.domain.reservation_admin.dto.response;

import java.time.LocalDateTime;

/**
 * [예약 관리 ->예약 상세 조회] 응답 DTO (Java Record)
 * * 단일 예약 건의 상세 정보를 클라이언트에게 전달
 * * 사용자 정보, 예약 목적, 시간, 승인 가능 여부 등의 상세 데이터가 포함
 */
public record ReservationDetailResponseDto(
	/**
	 * 예약 고유 ID
	 */
	Long reservationId,

	/**
	 * 예약된 공간 이름
	 */
	String spaceName,

	/**
	 * 예약한 사용자 요약 정보 (UserSummaryDto)
	 */
	UserSummaryDto user,

	/**
	 * 예약 목적
	 */
	String reservationPurpose,

	/**
	 * 예약 인원수
	 */
	Integer reservationHeadcount,

	/**
	 * 예약 시작 시간
	 */
	LocalDateTime reservationFrom,

	/**
	 * 예약 종료 시간
	 */
	LocalDateTime reservationTo,

	/**
	 * 주문 ID (Order ID)
	 */
	String orderId,

	/**
	 * 현재 예약 상태명
	 */
	String reservationStatusName,

	/**
	 * 현재 관리자 역할/지역 기준으로 해당 예약을 승인할 수 있는지 여부 (승인 버튼 활성화 플래그)
	 */
	boolean isApprovable,

	/**
	 * 현재 관리자 역할/지역 기준으로 해당 예약을 반려할 수 있는지 여부 (반려 버튼 활성화 플래그)
	 */
	boolean isRejectable,

	/**
	 * 현재 예약 상태 ID
	 */
	Integer statusId
) {
}
