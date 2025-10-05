package Team_Mute.back_end.domain.dashboard_admin.dto;

import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 대시보드 캘린더 보기에 사용될 예약 정보를 반환하는 DTO
 * 캘린더에 표시될 핵심 정보를 포함하며, {@code ReservationListResponseDto}로부터 변환 가능
 * Lombok 어노테이션을 사용하여 boilerplate 코드를 단축
 */
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

	/**
	 * {@code ReservationListResponseDto}를 {@code ReservationCalendarResponseDto}로 변환하는 정적 팩토리 메서드
	 * 캘린더 표시에 필요한 핵심 필드만 추출하여 새로운 DTO 객체를 생성
	 *
	 * @param dto 변환할 예약 리스트 응답 DTO
	 * @return 캘린더 형식에 맞는 예약 정보 DTO
	 */
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
