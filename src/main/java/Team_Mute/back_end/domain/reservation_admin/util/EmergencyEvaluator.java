package Team_Mute.back_end.domain.reservation_admin.util;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.global.constants.ReservationStatusEnum;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 예약이 '긴급' 상태인지 여부를 판단하는 평가자 클래스
 * 긴급 기준은 예약일 임박(5 영업일 이내) 또는 대기 시간 경과(5 영업일 이상 대기)
 */
@Component
public class EmergencyEvaluator {

	// 긴급 예약 판별 대상 상태 (승인 대기 중인 예약만 -> 1차 승인 대기, 2차 승인 대기)
	private static final Set<String> PENDING_STATUS = Set.of(ReservationStatusEnum.WAITING_FIRST_APPROVAL.getDescription(), ReservationStatusEnum.WAITING_SECOND_APPROVAL.getDescription());
	// 임박/오래대기 기준 영업일 수
	private static final int THRESHOLD = 5;
	// 시스템 타임존 (서울 기준)
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	/**
	 * 예약이 '긴급' 상태에 해당하는지 여부를 판단
	 * <p>
	 * 조건:
	 * 1) 예약일 임박: 오늘(제외)~예약일(포함) 영업일 <= 5  (오늘/주말 처리 포함)
	 * 2) 오래 대기: 접수일(제외)~오늘(포함) 영업일 >= 5
	 * 단, 승인 대기 상태(PENDING)인 예약만 대상
	 *
	 * @param r                     Reservation 엔티티
	 * @param reservationStatusName 현재 예약 상태 이름
	 * @return 긴급 예약이면 true
	 */
	public boolean isEmergency(Reservation r, String reservationStatusName) {
		// 1. 긴급 예약 판별 대상 상태인지 확인
		if (reservationStatusName == null || !PENDING_STATUS.contains(reservationStatusName)) {
			return false;
		}
		LocalDate today = LocalDate.now(ZONE);

		// 2. 예약일 임박 체크
		boolean dueSoon = false;
		if (r.getReservationFrom() != null) {
			LocalDate eventDate = r.getReservationFrom().atZone(ZONE).toLocalDate();
			long daysToEvent = BusinessDayUtils.businessDaysUntil(today, eventDate);
			// 예: 금요일(today) → 토요일(event): 0 (주말 제외), 0 <= 5 이므로 임박
			dueSoon = (daysToEvent >= 0) && (daysToEvent <= THRESHOLD);
		}

		// 3. 오래 대기 체크
		boolean waitingLong = false;
		if (r.getRegDate() != null) {
			LocalDate registered = r.getRegDate().atZone(ZONE).toLocalDate();
			long waited = BusinessDayUtils.businessDaysElapsed(registered, today);
			waitingLong = waited >= THRESHOLD;
		}

		// 4. 둘 중 하나라도 만족하면 긴급
		return dueSoon || waitingLong;
	}
}
