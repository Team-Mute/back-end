package Team_Mute.back_end.domain.reservation_admin.dto.response;

import Team_Mute.back_end.domain.reservation.entity.Reservation;

import java.time.LocalDateTime;
import java.util.List;

public class ReservationResponseDto {
	public Long reservationId;
	public String reservationStatusName;  // 사람이 읽을 수 있는 상태명
	public Integer reservationHeadcount;
	public LocalDateTime reservationFrom;
	public LocalDateTime reservationTo;
	public String reservationPurpose;
	public String reservationAttachment;
	public String orderId;
	public boolean canceledByUser;
	public String cancelReason;
	public LocalDateTime regDate;
	public LocalDateTime updDate;
	public List<PrevisitItemResponseDto> previsits;

	public static ReservationResponseDto from(
		Reservation r,
		String statusName,
		java.util.List<PrevisitItemResponseDto> previsitDtos
	) {
		ReservationResponseDto res = new ReservationResponseDto();
		res.reservationId = r.getReservationId();
		res.reservationStatusName = statusName;
		res.reservationHeadcount = r.getReservationHeadcount();
		res.reservationFrom = r.getReservationFrom();
		res.reservationTo = r.getReservationTo();
		res.regDate = r.getRegDate();
		res.previsits = previsitDtos;
		return res;
	}
}
