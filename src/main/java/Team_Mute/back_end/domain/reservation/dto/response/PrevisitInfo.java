package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.LocalDateTime;

import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import lombok.Builder;
import lombok.Data;

/**
 * 사전답사 정보 응답 DTO
 * 예약의 사전답사 정보를 클라이언트에 전달
 *
 * 사용 목적:
 * - 본 예약 전 공간을 미리 방문하는 사전답사 예약 정보
 * - 공간 상태 확인, 배치 계획 수립 목적
 * - 예약 상세 조회 및 목록 조회 시 포함
 *
 * 특징:
 * - 본 예약(Reservation)과 OneToOne 관계
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@Builder
public class PrevisitInfo {
	/**
	 * 사전답사 ID
	 * - PrevisitReservation 엔티티의 고유 식별자
	 * - 사전답사 수정 또는 삭제 시 사용
	 */
	private Long previsitId;

	/**
	 * 사전답사 시작 시간
	 * - 사전답사 시작 일시
	 * - 본 예약(reservationFrom) 이전
	 */
	private LocalDateTime previsitFrom;

	/**
	 * 사전답사 종료 시간
	 * - 사전답사 종료 일시
	 * - previsitFrom보다 이후
	 */
	private LocalDateTime previsitTo;

	/**
	 * PrevisitReservation 엔티티로부터 PrevisitInfo DTO 생성
	 * - 정적 팩토리 메서드
	 * - null-safe: previsit이 null이면 null 반환
	 *
	 * @param previsit PrevisitReservation 엔티티 (null 허용)
	 * @return PrevisitInfo DTO (previsit이 null이면 null 반환)
	 */
	public static PrevisitInfo fromEntity(PrevisitReservation previsit) {
		// 사전답사가 없는 경우 null 반환
		if (previsit == null) {
			return null;
		}

		// PrevisitInfo DTO 생성 및 반환
		return PrevisitInfo.builder()
			.previsitId(previsit.getPrevisitId())
			.previsitFrom(previsit.getPrevisitFrom())
			.previsitTo(previsit.getPrevisitTo())
			.build();
	}
}
