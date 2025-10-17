package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservationDetailResponseDto {

	private Long reservationId;
	private String orderId;
	private String spaceImageUrl; // 대표 이미지 URL
	private String spaceName;
	private LocalDateTime reservationFrom;
	private LocalDateTime reservationTo;
	private Integer reservationHeadcount;
	private String reservationPurpose;
	private PrevisitInfo previsits;
	private List<String> reservationAttachment;

	public static ReservationDetailResponseDto fromEntity(Reservation reservation) {
		String mainImageUrl = reservation.getSpace().getSpaceImageUrl(); // 대표 이미지가 없으면 null
		PrevisitReservation previsit = reservation.getPrevisitReservation(); // OneToOne or appropriate 매핑 필드
		PrevisitInfo previsitDto = PrevisitInfo.fromEntity(previsit);

		return ReservationDetailResponseDto.builder()
			.reservationId(reservation.getReservationId())
			.orderId(reservation.getOrderId())
			.spaceImageUrl(mainImageUrl)
			.spaceName(reservation.getSpace().getSpaceName())
			.reservationFrom(reservation.getReservationFrom())
			.reservationTo(reservation.getReservationTo())
			.reservationHeadcount(reservation.getReservationHeadcount())
			.reservationPurpose(reservation.getReservationPurpose())
			.previsits(previsitDto)
			.reservationAttachment(reservation.getReservationAttachment())
			.build();
	}
}
