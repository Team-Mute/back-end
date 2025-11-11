package Team_Mute.back_end.domain.reservation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 예약 가능 시간 조회 요청 DTO
 * 특정 날짜의 예약 가능한 시간대 목록을 조회하기 위한 요청 데이터
 *
 * 사용 목적:
 * - 프론트엔드 캘린더 UI에서 시간 선택 UI 제공
 * - 특정 날짜에 예약 가능한 시간대만 활성화
 * - 기존 예약과 겹치지 않는 시간대 표시
 *
 * API 엔드포인트:
 * - POST /api/reservations/available-times
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@Setter
public class AvailableTimeRequest {

	/**
	 * 공간 ID (필수)
	 * - 예약 가능 시간을 조회할 공간의 고유 식별자
	 * - Space 엔티티의 spaceId 참조
	 * - 공간마다 운영 시간(openTime, closeTime)이 다름
	 */
	@NotNull(message = "공간 ID는 필수입니다.")
	private Integer spaceId;

	/**
	 * 년도 (필수)
	 * - 조회할 연도
	 * - 2000년 이상만 허용
	 */
	@NotNull(message = "년도는 필수입니다.")
	@Min(2000)
	private Integer year;

	/**
	 * 월 (필수)
	 * - 조회할 월
	 * - 1~12 범위 (1월~12월)
	 */
	@NotNull(message = "월은 필수입니다.")
	@Min(1)
	@Max(12)
	private Integer month;

	/**
	 * 일 (필수)
	 * - 조회할 일
	 * - 1~31 범위
	 * - 해당 날짜의 시간별 예약 가능 여부 확인
	 */
	@NotNull(message = "일은 필수입니다.")
	@Min(1)
	@Max(31)
	private Integer day;
}
