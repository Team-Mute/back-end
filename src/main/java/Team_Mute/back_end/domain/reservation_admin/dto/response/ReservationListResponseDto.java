package Team_Mute.back_end.domain.reservation_admin.dto.response;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationListResponseDto {
	public Long reservationId;
	public String reservationStatusName;
	public String spaceName;
	public String userName;
	public Integer reservationHeadcount;
	public LocalDateTime reservationFrom;
	public LocalDateTime reservationTo;
	public LocalDateTime regDate;
	public boolean isShinhan;
	public boolean isEmergency;
	public boolean isApprovable;
	public boolean isRejectable;
	//public List<PrevisitItemResponseDto> previsits;

	// 예약 관리 필터링을 위해 추가한 필드
	private Integer regionId;
	private Long statusId;

	public static ReservationListResponseDto from(
		Reservation r,
		String statusName,
		String spaceName,
		String userName,
		boolean isShinhan,
		boolean isEmergency,
		boolean isApprovable,
		boolean isRejectable
	) {
		return ReservationListResponseDto.builder()
			.reservationId(r.getReservationId())
			.reservationStatusName(statusName)
			.spaceName(spaceName)
			.userName(userName)
			.reservationHeadcount(r.getReservationHeadcount())
			.reservationFrom(r.getReservationFrom())
			.reservationTo(r.getReservationTo())
			.regDate(r.getRegDate())
			.isShinhan(isShinhan)
			.isEmergency(isEmergency)
			.isApprovable(isApprovable)
			.isRejectable(isRejectable)
			// DTO에 누락된 필드들을 추가
			.regionId(r.getSpace().getRegionId())
			.statusId(r.getReservationStatus().getReservationStatusId())
			.build();
	}

	public Boolean getIsEmergency() {
		return isEmergency;
	}

	public Boolean getIsShinhan() {
		return isShinhan;
	}
}
