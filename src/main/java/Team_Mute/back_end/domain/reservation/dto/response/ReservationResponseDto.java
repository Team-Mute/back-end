package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationResponseDto {
	private Long reservationId; // 숫자 PK
	private String orderId;     // 고유 주문 ID
	private Integer spaceId;
	private String spaceName;
	private Long userId;
	private String userName;
	private Integer reservationStatusId;
	private String reservationStatusName;
	private Integer reservationHeadcount;
	private LocalDateTime reservationFrom;
	private LocalDateTime reservationTo;
	private String reservationPurpose;
	private List<String> reservationAttachment;
	private LocalDateTime regDate;
	private LocalDateTime updDate;

	private PrevisitInfo previsit;

	public static ReservationResponseDto fromEntity(Reservation reservation) {
		PrevisitReservation previsit = reservation.getPrevisitReservation(); // OneToOne or appropriate 매핑 필드
		PrevisitInfo previsitDto = PrevisitInfo.fromEntity(previsit);

		return ReservationResponseDto.builder()
			.reservationId(reservation.getReservationId())
			.orderId(reservation.getOrderId())
			.spaceId(reservation.getSpace().getSpaceId())
			.spaceName(reservation.getSpace().getSpaceName())
			.userId(reservation.getUser().getUserId())
			.userName(reservation.getUser().getUserName())
			.reservationStatusId(reservation.getReservationStatus().getReservationStatusId())
			.reservationStatusName(reservation.getReservationStatus().getReservationStatusName())
			.reservationHeadcount(reservation.getReservationHeadcount())
			.reservationFrom(reservation.getReservationFrom())
			.reservationTo(reservation.getReservationTo())
			.reservationPurpose(reservation.getReservationPurpose())
			.reservationAttachment(reservation.getReservationAttachment())
			.regDate(reservation.getRegDate())
			.updDate(reservation.getUpdDate())
			.previsit(previsitDto)
			.build();
	}
}
