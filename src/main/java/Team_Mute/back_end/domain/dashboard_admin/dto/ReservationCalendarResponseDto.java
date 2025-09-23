package Team_Mute.back_end.domain.dashboard_admin.dto;

import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationCalendarResponseDto {
	private Long reservationId;
	private String reservationStatusName;
	private String userName;
	private LocalDateTime reservationFrom;
	private LocalDateTime reservationTo;

	public static ReservationCalendarResponseDto from(ReservationListResponseDto dto) {
		return ReservationCalendarResponseDto.builder()
			.reservationId(dto.getReservationId())
			.reservationStatusName(dto.getReservationStatusName())
			.userName(dto.getUserName())
			.reservationFrom(dto.getReservationFrom())
			.reservationTo(dto.getReservationTo())
			.build();
	}
}
