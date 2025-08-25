package Team_Mute.back_end.domain.reservation_admin.util;


import java.time.DayOfWeek;
import java.time.LocalDate;

public final class BusinessDayUtils {
	private BusinessDayUtils() {
	}

	/**
	 * 주말 제외 영업일 여부
	 */
	public static boolean isBusinessDay(LocalDate d) {
		DayOfWeek w = d.getDayOfWeek();
		return w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY;
	}

	/**
	 * start(제외) ~ end(포함) 사이의 영업일 수.
	 * 예) Mon -> Fri = 4 (Tue,Wed,Thu,Fri)
	 * Fri -> Mon = 1 (Mon)
	 * today -> today = 0
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
			d = d.plusDays(1);           // 시작일 '제외' → 다음 날부터
			if (isBusinessDay(d)) count++; // 종료일 '포함' → 마지막 날 포함 체크
		}
		return reverse ? -count : count;
	}

	// 오늘(제외)부터 target(포함)까지 남은 영업일 수. 음수면 이미 지남.
	public static long businessDaysUntil(LocalDate today, LocalDate target) {
		return businessDaysBetweenExclIncl(today, target);
	}

	// startedAt(제외)부터 today(포함)까지 경과한 영업일 수.
	public static long businessDaysElapsed(LocalDate startedAt, LocalDate today) {
		return businessDaysBetweenExclIncl(startedAt, today);
	}
}
