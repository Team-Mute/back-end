package Team_Mute.back_end.domain.reservation.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 예약 가능 날짜 조회 응답 DTO
 * 특정 월의 예약 가능한 날짜(일) 목록 반환
 *
 * 사용 목적:
 * - 프론트엔드 캘린더 UI에서 예약 가능한 날짜 표시
 * - 이미 예약이 꽉 찬 날짜는 비활성화 처리
 * - 과거 날짜 제외
 *
 * API 엔드포인트:
 * - POST /api/reservations/available-dates
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class AvailableDateResponse {
	/**
	 * 예약 가능한 날짜(일) 리스트
	 * - 특정 월의 예약 가능한 날짜만 포함
	 * - 각 요소는 1~31 범위의 정수 (일)
	 * - 정렬된 순서로 반환 (오름차순)
	 * - 빈 리스트 가능 (모든 날짜가 예약 불가능한 경우)
	 */
	private List<Integer> availableDays;
}
