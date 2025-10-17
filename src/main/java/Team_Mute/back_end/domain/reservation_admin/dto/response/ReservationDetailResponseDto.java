package Team_Mute.back_end.domain.reservation_admin.dto.response;

import java.time.LocalDateTime;

public record ReservationDetailResponseDto(
	Long reservationId,
	String spaceName,
	UserSummaryDto user,
	String reservationPurpose,
	Integer reservationHeadcount,
	LocalDateTime reservationFrom,
	LocalDateTime reservationTo,
	String orderId,
	String reservationStatusName,
	boolean isApprovable,
	boolean isRejectable,
	Integer statusId
) {
}
