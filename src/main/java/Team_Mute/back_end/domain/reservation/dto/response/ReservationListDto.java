package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservationListDto {
	private Long reservationId;
	private String reservationStatusName;
	private String orderId;
	private String spaceName;
	private LocalDateTime reservationFrom;
	private LocalDateTime reservationTo;
	private List<PrevisitInfo> previsits; // 사전 방문 목록

	public static ReservationListDto fromEntity(Reservation reservation) {
		return ReservationListDto.builder()
			.reservationId(reservation.getReservationId())
			.reservationStatusName(mapStatusIdToName(reservation.getReservationStatus().getReservationStatusId()))
			.orderId(reservation.getOrderId())
			.spaceName(reservation.getSpace().getSpaceName()) // Space 엔티티에서 이름 가져오기
			.reservationFrom(reservation.getReservationFrom())
			.reservationTo(reservation.getReservationTo())
			.previsits(reservation.getPrevisitReservations().stream()
				.map(PrevisitInfo::fromEntity)
				.collect(Collectors.toList()))
			.build();
	}

	private static String mapStatusIdToName(Integer statusId) {
		return switch (statusId) {
			case 1, 2 -> "진행중";
			case 3 -> "예약완료";
			case 4 -> "반려";
			case 5 -> "이용완료";
			case 6 -> "예약취소";
			default -> "알 수 없음";
		};
	}
}
