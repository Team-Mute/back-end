package Team_Mute.back_end.domain.reservation.dto.response;

import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 예약 목록 항목 DTO
 * 예약 목록 조회 시 각 예약의 요약 정보 전달
 * <p>
 * 사용 목적:
 * - 마이페이지 예약 목록에서 사용
 * - 상세 정보는 제외하고 핵심 정보만 포함
 * - 페이징된 목록 조회 시 사용
 * <p>
 * ReservationDetailResponseDto와의 차이:
 * - 목록용: 핵심 정보만 (ID, 상태, 공간명, 시간)
 * - 상세용: 전체 정보 (첨부파일, 목적, 인원 등)
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@Builder
public class ReservationListDto {
	/**
	 * 예약 ID
	 * - 예약의 고유 식별자
	 */
	private Long reservationId;

	/**
	 * 예약 상태명
	 * - 현재 예약 상태의 이름 (한글)
	 * - statusId를 이름으로 매핑
	 * <p>
	 * 상태 매핑:
	 * - 1, 2: "진행중" (1차 승인 대기, 2차 승인 대기)
	 * - 3: "예약완료" (최종 승인)
	 * - 4: "반려"
	 * - 5: "이용완료"
	 * - 6: "예약취소"
	 */
	private String reservationStatusName;

	/**
	 * 주문 ID
	 * - 예약의 고유 주문 번호
	 */
	private String orderId;

	/**
	 * 공간 ID
	 * - 예약한 공간의 ID
	 */
	private Integer spaceId;

	/**
	 * 공간명
	 * - 예약한 공간의 이름
	 */
	private String spaceName;

	/**
	 * 예약 시작 시간
	 * - 예약 시작 일시
	 */
	private LocalDateTime reservationFrom;

	/**
	 * 예약 종료 시간
	 * - 예약 종료 일시
	 */
	private LocalDateTime reservationTo;

	/**
	 * 사전답사 정보
	 * - 사전 방문 예약 정보
	 * - PrevisitInfo DTO
	 * - null 가능
	 */
	private PrevisitInfo previsits;

	/**
	 * Reservation 엔티티로부터 ReservationListDto 생성
	 * - 정적 팩토리 메서드
	 * - 목록용 DTO로 변환
	 * <p>
	 * 처리 로직:
	 * 1. PrevisitReservation을 PrevisitInfo DTO로 변환
	 * 2. statusId를 상태명으로 매핑 (mapStatusIdToName)
	 * 3. Builder 패턴으로 DTO 생성
	 *
	 * @param reservation Reservation 엔티티
	 * @return ReservationListDto 인스턴스
	 */
	public static ReservationListDto fromEntity(Reservation reservation) {
		// 1. 사전답사 정보 변환
		PrevisitReservation previsit = reservation.getPrevisitReservation();
		PrevisitInfo previsitDto = PrevisitInfo.fromEntity(previsit);

		// 2. ReservationListDto 생성 및 반환
		return ReservationListDto.builder()
			.reservationId(reservation.getReservationId())
			.reservationStatusName(mapStatusIdToName(reservation.getReservationStatus().getReservationStatusId()))
			.orderId(reservation.getOrderId())
			.spaceId(reservation.getSpace().getSpaceId())
			.spaceName(reservation.getSpace().getSpaceName())
			.reservationFrom(reservation.getReservationFrom())
			.reservationTo(reservation.getReservationTo())
			.previsits(previsitDto)
			.build();
	}

	/**
	 * 예약 상태 ID를 상태명으로 매핑
	 * - statusId를 한글 상태명으로 변환
	 * - switch 표현식 사용
	 * <p>
	 * 매핑 규칙:
	 * - 1: 1차 승인 대기 → "진행중"
	 * - 2: 2차 승인 대기 → "진행중"
	 * - 3: 최종 승인 → "예약완료"
	 * - 4: 반려 → "반려"
	 * - 5: 이용 완료 → "이용완료"
	 * - 6: 취소 → "예약취소"
	 * - 기타: "알 수 없음"
	 *
	 * @param statusId 예약 상태 ID
	 * @return 상태명 (한글)
	 */
	private static String mapStatusIdToName(Integer statusId) {
		return switch (statusId.intValue()) {
			case 1, 2 -> "진행중";
			case 3 -> "예약완료";
			case 4 -> "반려";
			case 5 -> "이용완료";
			case 6 -> "예약취소";
			default -> "알 수 없음";
		};
	}
}
