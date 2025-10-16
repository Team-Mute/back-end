package Team_Mute.back_end.domain.reservation_admin.util;


import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 영업일(주말 제외) 계산 관련 유틸리티 클래스
 * 주말을 제외한 날짜 간의 영업일 수 등을 계산
 */
public final class BusinessDayUtils {
	private BusinessDayUtils() {
	}

	/**
	 * 해당 날짜가 주말(토, 일)을 제외한 영업일인지 여부를 판단
	 *
	 * @param d 확인할 LocalDate 객체
	 * @return 주말 제외 영업일 여부
	 */
	public static boolean isBusinessDay(LocalDate d) {
		DayOfWeek w = d.getDayOfWeek();
		return w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY;
	}

	/**
	 * {@code startExclusive} (제외)부터 {@code endInclusive} (포함) 사이의 영업일 수를 계산
	 * 예: Mon(제외) → Fri(포함) = 4 (Tue, Wed, Thu, Fri)
	 * Fri(제외) → Mon(포함) = 1 (Mon)
	 * today → today = 0
	 *
	 * @param startExclusive 시작 날짜 (계산에서 제외)
	 * @param endInclusive   종료 날짜 (계산에 포함)
	 * @return 두 날짜 사이의 영업일 수. {@code endInclusive}가 {@code startExclusive}보다 이전이면 음수 반환.
	 */
	public static long businessDaysBetweenExclIncl(LocalDate startExclusive, LocalDate endInclusive) {
		if (startExclusive == null || endInclusive == null) return 0L;
		if (endInclusive.isEqual(startExclusive)) return 0L;

		boolean reverse = false;
		LocalDate s = startExclusive;
		LocalDate e = endInclusive;
		if (e.isBefore(s)) { // 음수 구간도 지원
			LocalDate tmp = s;
			s = e;
			e = tmp;
			reverse = true;
		}

		long count = 0L;
		LocalDate d = s;
		while (!d.isEqual(e)) {
			d = d.plusDays(1);    // 시작일 '제외' → 다음 날부터
			if (isBusinessDay(d)) count++; // 종료일 '포함' → 마지막 날 포함 체크
		}
		return reverse ? -count : count;
	}

	/**
	 * 오늘({@code today}, 제외)부터 특정 날짜({@code target}, 포함)까지 남은 영업일 수를 계산
	 *
	 * @param today  오늘 날짜
	 * @param target 대상 날짜
	 * @return 남은 영업일 수. 음수면 이미 지났음을 의미.
	 */
	public static long businessDaysUntil(LocalDate today, LocalDate target) {
		return businessDaysBetweenExclIncl(today, target);
	}

	/**
	 * 시작일({@code startedAt}, 제외)부터 오늘({@code today}, 포함)까지 경과한 영업일 수를 계산
	 *
	 * @param startedAt 시작 날짜
	 * @param today     오늘 날짜
	 * @return 경과한 영업일 수.
	 */
	public static long businessDaysElapsed(LocalDate startedAt, LocalDate today) {
		return businessDaysBetweenExclIncl(startedAt, today);
	}
}
