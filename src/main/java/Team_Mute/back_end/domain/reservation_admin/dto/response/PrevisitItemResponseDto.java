package Team_Mute.back_end.domain.reservation_admin.dto.response;


import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;

import java.time.LocalDateTime;

/**
 * [예약 관리 리스트] 정보에 포함되는 사전 방문 예약 항목 DTO
 * * 예약 상세 정보 조회 시, 해당 예약에 연결된 사전 방문 기록 리스트의 각 항목을 나타냅니다.
 */
public class PrevisitItemResponseDto {
	/**
	 * 사전 답사 고유 ID
	 */
	public Long previsitId;

	/**
	 * 사전 답사 시작 일시
	 */
	public LocalDateTime previsitFrom;

	/**
	 * 사전 답사 종료 일시
	 */
	public LocalDateTime previsitTo;

	/**
	 * {@code PrevisitReservation} 엔티티를 {@code PrevisitItemResponseDto}로 변환
	 *
	 * @param p 사전 방문 예약 엔티티
	 * @return 변환된 DTO
	 */
	public static PrevisitItemResponseDto from(PrevisitReservation p) {
		PrevisitItemResponseDto res = new PrevisitItemResponseDto();
		res.previsitId = p.getPrevisitId();
		res.previsitFrom = p.getPrevisitFrom();
		res.previsitTo = p.getPrevisitTo();
		return res;
	}
}
