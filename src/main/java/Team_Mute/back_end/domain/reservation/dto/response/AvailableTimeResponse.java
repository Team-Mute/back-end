package Team_Mute.back_end.domain.reservation.dto.response;

import java.time.LocalTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 예약 가능 시간 조회 응답 DTO
 * 특정 날짜의 예약 가능한 시간대 목록 반환
 *
 * 사용 목적:
 * - 프론트엔드에서 시간 선택 UI 제공
 * - 특정 날짜의 시간별 예약 가능 여부 표시
 * - 기존 예약과 겹치지 않는 시간대만 선택 가능
 *
 * API 엔드포인트:
 * - POST /api/reservations/available-times
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class AvailableTimeResponse {
	/**
	 * 예약 가능한 시간대 리스트
	 * - 각 요소는 TimeSlot 객체 (시작 시간, 종료 시간)
	 * - 1시간 단위 또는 30분 단위 (설정에 따라 다름)
	 * - 정렬된 순서로 반환 (시간 순)
	 * - 빈 리스트 가능 (모든 시간대가 예약 불가능한 경우)
	 */
	private List<TimeSlot> availableTimes;

	/**
	 * 시간대 중첩 클래스
	 * 예약 가능한 하나의 시간대를 나타냄
	 */
	@Getter
	@AllArgsConstructor
	public static class TimeSlot {
		/**
		 * 시간대 시작 시간
		 * - LocalTime 타입 (시:분)
		 * - endTime보다 이전
		 */
		private LocalTime startTime;

		/**
		 * 시간대 종료 시간
		 * - LocalTime 타입 (시:분)
		 * - startTime보다 이후
		 */
		private LocalTime endTime;
	}
}
