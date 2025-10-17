package Team_Mute.back_end.domain.reservation_admin.dto.response;

import java.time.LocalDateTime;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * [예약 관리 리스트] 개별 항목 응답 DTO
 * * 예약 목록 테이블의 각 행에 표시될 핵심 정보들을 담고 있
 * * 예약 ID, 상태, 시간, 사용자 정보, 각종 플래그 및 승인 가능 여부가 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationListResponseDto {
	/**
	 * 예약 고유 ID
	 */
	public Long reservationId;

	/**
	 * 현재 예약 상태 이름
	 */
	public String reservationStatusName;

	/**
	 * 예약된 공간 이름
	 */
	public String spaceName;

	/**
	 * 예약한 사용자 이름
	 */
	public String userName;

	/**
	 * 예약 인원수
	 */
	public Integer reservationHeadcount;

	/**
	 * 예약 시작 시간
	 */
	public LocalDateTime reservationFrom;

	/**
	 * 예약 종료 시간
	 */
	public LocalDateTime reservationTo;

	/**
	 * 예약 접수일
	 */
	public LocalDateTime regDate;

	/**
	 * 신한 그룹 관련 예약 여부 플래그
	 */
	public boolean isShinhan;

	/**
	 * 긴급 예약 (임박/오래 대기) 여부 플래그
	 */
	public boolean isEmergency;

	/**
	 * 현재 관리자 역할/지역 기준으로 승인 가능 여부
	 */
	public boolean isApprovable;

	/**
	 * 현재 관리자 역할/지역 기준으로 반려 가능 여부
	 */
	public boolean isRejectable;

	// 예약 관리 필터링을 위해 추가한 필드
	/**
	 * 예약된 공간의 지역 ID (필터링 및 권한 체크용)
	 */
	private Integer regionId;

	/**
	 * 예약 상태 ID (필터링 및 정렬용)
	 */
	private Integer statusId;

	/**
	 * Reservation 엔티티와 추가 정보를 이용해 DTO를 생성하는 팩토리 메서드.
	 */
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
