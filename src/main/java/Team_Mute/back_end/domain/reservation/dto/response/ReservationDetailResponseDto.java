package Team_Mute.back_end.domain.reservation.dto.response;

import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 상세 조회 응답 DTO
 * 특정 예약의 상세 정보를 클라이언트에 전달
 * <p>
 * 사용 목적:
 * - 예약 상세 페이지에서 전체 정보 표시
 * - 공간 정보, 예약 시간, 인원, 목적, 첨부파일 등 포함
 * - 사전답사 정보 포함 (있는 경우)
 * <p>
 * API 엔드포인트:
 * - GET /api/reservations/{reservationId}
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@Builder
public class ReservationDetailResponseDto {

	/**
	 * 예약 ID
	 * - 예약의 고유 식별자 (숫자 PK)
	 * - Reservation 엔티티의 reservationId
	 */
	private Long reservationId;

	/**
	 * 주문 ID
	 * - 예약의 고유 주문 번호 (문자열)
	 * - 사용자 친화적 식별자
	 */
	private String orderId;

	/**
	 * 공간 대표 이미지 URL
	 * - 예약한 공간의 대표 이미지
	 * - AWS S3에 저장된 이미지 URL
	 */
	private String spaceImageUrl;

	/**
	 * 공간 ID
	 * - 예약한 공간의 ID
	 */
	private Integer spaceId;

	/**
	 * 공간명
	 * - 예약한 공간의 이름
	 * - Space 엔티티의 spaceName
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
	 * 예약 인원
	 * - 참석 예정 인원 수
	 */
	private Integer reservationHeadcount;

	/**
	 * 예약 목적
	 * - 예약 사유 또는 행사 내용
	 */
	private String reservationPurpose;

	/**
	 * 사전답사 정보
	 * - 본 예약 전 공간 방문 정보
	 * - PrevisitInfo DTO
	 * - null 가능 (사전답사가 없는 경우)
	 */
	private PrevisitInfo previsits;

	/**
	 * 첨부 파일 URL 리스트
	 * - 예약 시 업로드한 첨부 파일의 S3 URL
	 * - 빈 리스트 가능 (첨부 파일이 없는 경우)
	 */
	private List<String> reservationAttachment;

	/**
	 * Reservation 엔티티로부터 ReservationDetailResponseDto 생성
	 * - 정적 팩토리 메서드
	 * - 엔티티를 DTO로 변환
	 * <p>
	 * 처리 로직:
	 * 1. Space 엔티티에서 대표 이미지 URL 추출
	 * 2. PrevisitReservation 엔티티를 PrevisitInfo DTO로 변환
	 * 3. Builder 패턴으로 DTO 생성
	 *
	 * @param reservation Reservation 엔티티
	 * @return ReservationDetailResponseDto 인스턴스
	 */
	public static ReservationDetailResponseDto fromEntity(Reservation reservation) {
		// 1. 공간 대표 이미지 URL 추출 (없으면 null)
		String mainImageUrl = reservation.getSpace().getSpaceImageUrl();

		// 2. 사전답사 정보 추출 및 DTO 변환
		PrevisitReservation previsit = reservation.getPrevisitReservation();
		PrevisitInfo previsitDto = PrevisitInfo.fromEntity(previsit);

		// 3. ReservationDetailResponseDto 생성 및 반환
		return ReservationDetailResponseDto.builder()
			.reservationId(reservation.getReservationId())
			.orderId(reservation.getOrderId())
			.spaceImageUrl(mainImageUrl)
			.spaceId(reservation.getSpace().getSpaceId())
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
