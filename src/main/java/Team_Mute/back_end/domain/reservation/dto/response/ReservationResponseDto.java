package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Getter;

/**
 * 예약 생성 응답 DTO
 * 예약 생성 성공 시 생성된 예약의 전체 정보 반환
 *
 * 사용 목적:
 * - 예약 생성 API 응답
 * - 생성된 예약의 모든 정보 확인
 * - 프론트엔드에서 생성 결과 표시
 *
 * API 엔드포인트:
 * - POST /api/reservations
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@Builder
public class ReservationResponseDto {
	/**
	 * 예약 ID
	 * - 생성된 예약의 고유 식별자
	 */
	private Long reservationId;

	/**
	 * 주문 ID
	 * - 예약의 고유 주문 번호 (문자열)
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
	 * 사용자 ID
	 * - 예약한 사용자의 ID
	 */
	private Long userId;

	/**
	 * 사용자 이름
	 * - 예약한 사용자의 이름
	 */
	private String userName;

	/**
	 * 예약 상태 ID
	 * - 현재 예약 상태의 ID
	 * - 초기 상태: 1 (1차 승인 대기)
	 */
	private Integer reservationStatusId;

	/**
	 * 예약 상태명
	 * - 현재 예약 상태의 이름
	 */
	private String reservationStatusName;

	/**
	 * 예약 인원
	 * - 참석 예정 인원 수
	 */
	private Integer reservationHeadcount;

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
	 * 예약 목적
	 * - 예약 사유 또는 행사 내용
	 */
	private String reservationPurpose;

	/**
	 * 첨부 파일 URL 리스트
	 * - 업로드된 첨부 파일의 S3 URL
	 */
	private List<String> reservationAttachment;

	/**
	 * 등록 일시
	 * - 예약이 생성된 일시
	 */
	private LocalDateTime regDate;

	/**
	 * 수정 일시
	 * - 예약이 마지막으로 수정된 일시
	 */
	private LocalDateTime updDate;

	/**
	 * 사전답사 정보
	 * - 사전 방문 예약 정보
	 * - null 가능
	 */
	private PrevisitInfo previsit;

	/**
	 * Reservation 엔티티로부터 ReservationResponseDto 생성
	 * - 정적 팩토리 메서드
	 * - 생성된 예약을 응답 DTO로 변환
	 *
	 * @param reservation Reservation 엔티티
	 * @return ReservationResponseDto 인스턴스
	 */
	public static ReservationResponseDto fromEntity(Reservation reservation) {
		// 사전답사 정보 변환
		PrevisitReservation previsit = reservation.getPrevisitReservation();
		PrevisitInfo previsitDto = PrevisitInfo.fromEntity(previsit);

		// ReservationResponseDto 생성 및 반환
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
