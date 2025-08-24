package Team_Mute.back_end.domain.reservation_admin.util;

import Team_Mute.back_end.domain.reservation_admin.entity.Reservation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EmergencyEvaluator {

	// 승인 대기 상태만 긴급 후보 
	private static final Set<String> PENDING_STATUS = Set.of("1차 승인 대기", "2차 승인 대기");
	// 임박/오래대기 기준(영업일)
	private static final int THRESHOLD = 5;
	// 시스템 타임존(요구: Asia/Seoul)
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	/**
	 * 조건:
	 * 1) 예약일 임박: 오늘(제외)~예약일(포함) 영업일 <= 5  (오늘/주말 처리 포함)
	 * 2) 오래 대기: 접수일(제외)~오늘(포함) 영업일 >= 5
	 * 단, 승인 대기 상태(PENDING)인 예약만 대상
	 */
	public boolean isEmergency(Reservation r, String reservationStatusName) {
		if (reservationStatusName == null || !PENDING_STATUS.contains(reservationStatusName)) {
			return false;
		}
		LocalDate today = LocalDate.now(ZONE);

		boolean dueSoon = false;
		if (r.getReservationFrom() != null) {
			LocalDate eventDate = r.getReservationFrom().atZone(ZONE).toLocalDate();
			long daysToEvent = BusinessDayUtils.businessDaysUntil(today, eventDate);
			// 예: 금요일(today) → 토요일(event): 0 (주말 제외), 0 <= 5 이므로 임박
			dueSoon = (daysToEvent >= 0) && (daysToEvent <= THRESHOLD);
		}

		boolean waitingLong = false;
		if (r.getRegDate() != null) {
			LocalDate registered = r.getRegDate().atZone(ZONE).toLocalDate();
			long waited = BusinessDayUtils.businessDaysElapsed(registered, today);
			waitingLong = waited >= THRESHOLD;
		}

		return dueSoon || waitingLong;
	}
}
