package Team_Mute.back_end.domain.reservation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 예약 가능 날짜 조회 요청 DTO
 * 특정 월의 예약 가능한 날짜(일) 목록을 조회하기 위한 요청 데이터
 *
 * 사용 목적:
 * - 프론트엔드 캘린더 UI에서 예약 가능한 날짜 표시
 * - 특정 공간의 특정 월에 예약 가능한 날짜만 활성화
 * - 이미 예약이 꽉 찬 날짜는 비활성화 처리
 *
 * API 엔드포인트:
 * - POST /api/reservations/available-dates
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@Setter
public class AvailableDateRequest {
	/**
	 * 공간 ID (필수)
	 * - 예약 가능 날짜를 조회할 공간의 고유 식별자
	 * - Space 엔티티의 spaceId 참조
	 */
	@NotNull(message = "공간 ID는 필수입니다.")
	private Integer spaceId;

	/**
	 * 년도 (필수)
	 * - 조회할 연도
	 * - 2000년 이상만 허용 (현실적인 날짜 범위)
	 * - 과거 년도도 조회 가능 (이력 확인 목적)
	 */
	@NotNull(message = "년도는 필수입니다.")
	@Min(2000)
	private Integer year;

	/**
	 * 월 (필수)
	 * - 조회할 월
	 * - 1~12 범위 (1월~12월)
	 * - 해당 월의 모든 날짜 중 예약 가능한 날짜만 반환
	 */
	@NotNull(message = "월은 필수입니다.")
	@Min(1)
	@Max(12)
	private Integer month;
}
