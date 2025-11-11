package Team_Mute.back_end.domain.reservation.service;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Team_Mute.back_end.domain.reservation.dto.response.AvailableTimeResponse;
import Team_Mute.back_end.domain.reservation.dto.response.AvailableTimeResponse.TimeSlot;
import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.repository.PrevisitRepository;
import Team_Mute.back_end.domain.reservation.repository.ReservationRepository;
import Team_Mute.back_end.domain.space_admin.entity.SpaceClosedDay;
import Team_Mute.back_end.domain.space_admin.entity.SpaceOperation;
import Team_Mute.back_end.domain.space_admin.repository.SpaceClosedDayRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceOperationRepository;
import lombok.RequiredArgsConstructor;

/**
 * 예약 스케줄 서비스
 * 캘린더 UI를 위한 예약 가능 날짜/시간 조회 기능 제공
 * 공간 운영 시간, 휴무일, 기존 예약을 고려하여 예약 가능 시간대 계산
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationScheduleService {

	private final SpaceOperationRepository spaceOperationRepository;
	private final SpaceClosedDayRepository spaceClosedDayRepository;
	private final ReservationRepository reservationRepository;
	private final PrevisitRepository previsitRepository;

	/**
	 * 특정 날짜의 예약 가능 시간대 조회
	 *
	 * 처리 흐름:
	 * 1. 날짜 유효성 검증
	 * 2. 운영일/휴무일 확인
	 * 3. 해당 날짜의 운영 시간 조회
	 * 4. 기존 예약 시간 조회 (일반 예약 + 사전답사)
	 * 5. 운영 시간에서 예약된 시간 제외하여 가능 시간 계산
	 * 6. 과거 시간 필터링
	 *
	 * @param year 년도
	 * @param month 월
	 * @param day 일
	 * @param spaceId 공간 ID
	 * @return 예약 가능 시간대 리스트
	 * @throws InvalidInputValueException 운영일이 아니거나 휴무일인 경우
	 */
	public List<TimeSlot> getAvailableTimes(int year, int month, int day, int spaceId) {
		LocalDate requestedDate = validateAndGetDate(year, month, day);

		// 1. 운영일 및 휴무일 확인
		if (!isBookableDay(requestedDate, spaceId)) {
			throw new InvalidInputValueException("선택하신 날짜는 운영일이 아니거나 휴무일입니다.");
		}

		// 2. 해당 날짜의 운영 시간 조회
		SpaceOperation operation = getOperationForDay(requestedDate, spaceId);
		if (operation == null) {
			throw new InvalidInputValueException("해당 요일의 운영 정보를 찾을 수 없습니다.");
		}
		LocalTime operationStart = operation.getOperationFrom();
		LocalTime operationEnd = operation.getOperationTo();

		// 3. 해당 날짜의 예약된 시간 목록 조회 (일반 예약 + 사전답사)
		List<LocalTime[]> bookedSlots = getBookedSlotsForDay(requestedDate, spaceId);

		// 4. 예약 가능 시간대 계산 (운영 시간 - 예약된 시간)
		List<TimeSlot> allSlots = calculateAvailableTimeSlots(operationStart, operationEnd, bookedSlots);

		// 5. 오늘이면 과거 시간 제외, 과거 날짜면 빈 리스트 반환
		if (requestedDate.isEqual(LocalDate.now())) {
			LocalTime nowTime = LocalTime.now();
			return allSlots.stream()
				.filter(slot -> slot.getEndTime().isAfter(nowTime))
				.map(slot -> {
					LocalTime adjStart = slot.getStartTime().isBefore(nowTime) ? nowTime : slot.getStartTime();
					return new AvailableTimeResponse.TimeSlot(adjStart, slot.getEndTime());
				})
				.filter(slot -> slot.getStartTime().isBefore(slot.getEndTime()))
				.collect(Collectors.toList());
		} else if (requestedDate.isBefore(LocalDate.now())) {
			return Collections.emptyList();
		}
		return allSlots;
	}

	/**
	 * 특정 월의 예약 가능 날짜 조회
	 *
	 * 처리 흐름:
	 * 1. 운영 요일 기반으로 가능한 날짜 계산
	 * 2. 오늘 이전 날짜 제외
	 * 3. 지정된 휴무일 제외
	 * 4. 예약이 꽉 찬 날짜 제외
	 *
	 * @param year 년도
	 * @param month 월
	 * @param spaceId 공간 ID
	 * @return 예약 가능한 날짜(일) 리스트
	 */
	public List<Integer> getAvailableDays(int year, int month, int spaceId) {
		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDate today = LocalDate.now();

		LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
		LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

		// Step 1: 운영 요일 기반 가능 날짜 계산
		Set<Integer> openDays = getOpenDaysOfMonth(spaceId, yearMonth);

		// Step 2: 오늘 이전 날짜 제외
		openDays = openDays.stream()
			.filter(day -> !yearMonth.atDay(day).isBefore(today))
			.collect(Collectors.toSet());

		// Step 3: 휴무일 제외
		removeClosedDays(openDays, spaceId, startOfMonth, endOfMonth);

		// Step 4: 예약 꽉 찬 날짜 제외
		removeFullyBookedDays(openDays, spaceId, startOfMonth, endOfMonth, yearMonth);

		return openDays.stream().sorted().collect(Collectors.toList());
	}

	/**
	 * 운영 요일 기반으로 해당 월의 가능한 날짜 계산
	 * @return 운영 요일에 해당하는 날짜(일) Set
	 */
	private Set<Integer> getOpenDaysOfMonth(Integer spaceId, YearMonth yearMonth) {
		List<SpaceOperation> operations = spaceOperationRepository.findBySpace_SpaceId(spaceId);
		Set<DayOfWeek> openDayOfWeeks = operations.stream()
			.filter(SpaceOperation::getIsOpen)
			.map(op -> DayOfWeek.of(op.getDay()))
			.collect(Collectors.toSet());

		Set<Integer> openDays = new HashSet<>();
		for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
			LocalDate currentDate = yearMonth.atDay(day);
			if (openDayOfWeeks.contains(currentDate.getDayOfWeek())) {
				openDays.add(day);
			}
		}
		return openDays;
	}

	/**
	 * 휴무일 제외 처리
	 * @param openDays 가능 날짜 Set
	 */
	private void removeClosedDays(Set<Integer> openDays, Integer spaceId, LocalDateTime start, LocalDateTime end) {
		List<SpaceClosedDay> closedPeriods = spaceClosedDayRepository.findBySpaceIdAndMonth(spaceId, start, end);
		for (SpaceClosedDay period : closedPeriods) {
			LocalDate from = period.getClosedFrom().toLocalDate();
			LocalDate to = period.getClosedTo().toLocalDate();
			from.datesUntil(to.plusDays(1)).forEach(date -> {
				if (date.getMonthValue() == start.getMonthValue()) {
					openDays.remove(date.getDayOfMonth());
				}
			});
		}
	}

	/**
	 * 예약이 꽉 찬 날짜 제외 처리
	 * 일자별 예약 시간을 집계하여 운영 시간과 비교
	 * @param openDays 가능 날짜 Set
	 */
	private void removeFullyBookedDays(Set<Integer> openDays, Integer spaceId, LocalDateTime start, LocalDateTime end,
		YearMonth yearMonth) {
		Map<Integer, List<LocalTime[]>> dailyBookedTimes = new HashMap<>();

		// 1. 일반 예약 시간 집계
		List<Long> validStatusIds = Arrays.asList(1L, 2L, 3L);
		List<Reservation> reservations = reservationRepository.findReservationsBySpaceAndMonth(spaceId, start, end,
			validStatusIds);
		for (Reservation r : reservations) {
			addBookedTimes(dailyBookedTimes, r.getReservationFrom(), r.getReservationTo(), yearMonth);
		}

		// 2. 사전답사 시간 집계
		List<PrevisitReservation> previsits = previsitRepository.findValidPrevisitsBySpaceAndDay(spaceId, start, end,
			validStatusIds);
		for (PrevisitReservation pr : previsits) {
			addBookedTimes(dailyBookedTimes, pr.getPrevisitFrom(), pr.getPrevisitTo(), yearMonth);
		}

		// 3. 운영 시간과 비교하여 꽉 찬 날짜 제거
		Map<DayOfWeek, SpaceOperation> operationMap = getOperationMap(spaceId);

		for (int day : new ArrayList<>(openDays)) {
			LocalDate currentDate = yearMonth.atDay(day);
			DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
			SpaceOperation operation = operationMap.get(dayOfWeek);

			if (operation == null || !operation.getIsOpen())
				continue;

			long totalOperationMinutes = Duration.between(operation.getOperationFrom(), operation.getOperationTo())
				.toMinutes();

			List<LocalTime[]> bookedSlots = dailyBookedTimes.getOrDefault(day, Collections.emptyList());
			long totalBookedMinutes = calculateTotalBookedMinutes(bookedSlots);

			if (totalBookedMinutes >= totalOperationMinutes) {
				openDays.remove(day);
			}
		}
	}

	/**
	 * 예약 시간을 일자별 맵에 추가
	 */
	private void addBookedTimes(Map<Integer, List<LocalTime[]>> map, LocalDateTime from, LocalDateTime to,
		YearMonth yearMonth) {
		IntStream.rangeClosed(from.getDayOfMonth(), to.getDayOfMonth()).forEach(day -> {
			if (from.getYear() == yearMonth.getYear() && from.getMonth() == yearMonth.getMonth()) {
				LocalTime startTime = (day == from.getDayOfMonth()) ? from.toLocalTime() : LocalTime.MIN;
				LocalTime endTime = (day == to.getDayOfMonth()) ? to.toLocalTime() : LocalTime.MAX;
				map.computeIfAbsent(day, k -> new ArrayList<>()).add(new LocalTime[] {startTime, endTime});
			}
		});
	}

	/**
	 * 중복 시간 병합하여 총 예약 시간 계산
	 * @return 총 예약 시간 (분 단위)
	 */
	private long calculateTotalBookedMinutes(List<LocalTime[]> slots) {
		if (slots.isEmpty())
			return 0;
		slots.sort(Comparator.comparing(a -> a[0]));

		long totalMinutes = 0;
		LocalTime mergedStart = slots.get(0)[0];
		LocalTime mergedEnd = slots.get(0)[1];

		for (int i = 1; i < slots.size(); i++) {
			LocalTime[] current = slots.get(i);
			if (current[0].isBefore(mergedEnd)) { // 시간 겹침
				mergedEnd = mergedEnd.isAfter(current[1]) ? mergedEnd : current[1];
			} else { // 겹치지 않음
				totalMinutes += Duration.between(mergedStart, mergedEnd).toMinutes();
				mergedStart = current[0];
				mergedEnd = current[1];
			}
		}
		totalMinutes += Duration.between(mergedStart, mergedEnd).toMinutes();
		return totalMinutes;
	}

	/**
	 * 특정 날짜가 예약 가능한지 확인
	 * @return 운영일이면서 휴무일이 아니면 true
	 */
	private boolean isBookableDay(LocalDate date, int spaceId) {
		// 운영 요일 확인
		Map<DayOfWeek, SpaceOperation> operationMap = getOperationMap(spaceId);
		SpaceOperation operation = operationMap.get(date.getDayOfWeek());
		if (operation == null || !operation.getIsOpen()) {
			return false;
		}

		// 휴무일 확인
		LocalDateTime startOfDay = date.atStartOfDay();
		LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
		List<SpaceClosedDay> closedPeriods = spaceClosedDayRepository.findBySpaceIdAndMonth(spaceId, startOfDay,
			endOfDay);
		for (SpaceClosedDay period : closedPeriods) {
			if (!date.isBefore(period.getClosedFrom().toLocalDate()) && !date.isAfter(
				period.getClosedTo().toLocalDate())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 특정 날짜의 예약된 시간대 조회
	 * @return 예약된 시간 배열 리스트 [시작, 종료]
	 */
	private List<LocalTime[]> getBookedSlotsForDay(LocalDate date, int spaceId) {
		LocalDateTime startOfDay = date.atStartOfDay();
		LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

		List<LocalTime[]> bookedSlots = new ArrayList<>();
		List<Long> validStatusIds = Arrays.asList(1L, 2L, 3L);

		// 일반 예약 시간 추가
		List<Reservation> reservations = reservationRepository.findReservationsBySpaceAndMonth(spaceId, startOfDay,
			endOfDay, validStatusIds);
		for (Reservation r : reservations) {
			addBookedSlotForDay(bookedSlots, r.getReservationFrom(), r.getReservationTo(), date);
		}

		// 사전답사 시간 추가
		List<PrevisitReservation> previsits = previsitRepository.findValidPrevisitsBySpaceAndDay(spaceId, startOfDay,
			endOfDay, validStatusIds);
		for (PrevisitReservation pr : previsits) {
			addBookedSlotForDay(bookedSlots, pr.getPrevisitFrom(), pr.getPrevisitTo(), date);
		}

		return bookedSlots;
	}

	/**
	 * 공간 운영 정보 조회 (요일별 맵)
	 */
	private Map<DayOfWeek, SpaceOperation> getOperationMap(int spaceId) {
		return spaceOperationRepository.findBySpace_SpaceId(spaceId).stream()
			.collect(Collectors.toMap(
				op -> DayOfWeek.of(op.getDay()),
				op -> op,
				(op1, op2) -> op1
			));
	}

	/**
	 * 특정 날짜의 운영 정보 조회
	 */
	private SpaceOperation getOperationForDay(LocalDate date, int spaceId) {
		return getOperationMap(spaceId).get(date.getDayOfWeek());
	}

	/**
	 * 특정 날짜에 대한 예약 시간 슬롯 추가
	 * 나노초 오차 제거로 정확한 시간 비교 보장
	 */
	private void addBookedSlotForDay(List<LocalTime[]> slots, LocalDateTime from, LocalDateTime to,
		LocalDate targetDate) {
		if (from.toLocalDate().isEqual(targetDate) || to.toLocalDate().isEqual(targetDate) ||
			(from.toLocalDate().isBefore(targetDate) && to.toLocalDate().isAfter(targetDate))) {

			LocalTime startTime =
				from.toLocalDate().isEqual(targetDate) ? from.toLocalTime().withNano(0) : LocalTime.MIN;
			LocalTime endTime = to.toLocalDate().isEqual(targetDate) ? to.toLocalTime().withNano(0) : LocalTime.MAX;
			slots.add(new LocalTime[] {startTime, endTime});
		}
	}

	/**
	 * 예약 가능 시간대 계산
	 * 운영 시간에서 예약된 시간을 제외하여 빈 시간대 반환
	 * @return 예약 가능 TimeSlot 리스트
	 */
	private List<TimeSlot> calculateAvailableTimeSlots(LocalTime operationStart, LocalTime operationEnd,
		List<LocalTime[]> bookedSlots) {
		List<TimeSlot> availableSlots = new ArrayList<>();

		// 예약된 시간을 시작 시간 기준으로 정렬
		bookedSlots.sort(Comparator.comparing(a -> a[0]));

		LocalTime currentTime = operationStart;

		for (LocalTime[] slot : bookedSlots) {
			LocalTime bookedStart = slot[0];
			LocalTime bookedEnd = slot[1];

			// 현재 시간과 예약 시작 시간 사이 빈 공간 추가
			if (currentTime.isBefore(bookedStart)) {
				availableSlots.add(new TimeSlot(currentTime, bookedStart));
			}
			currentTime = currentTime.isAfter(bookedEnd) ? currentTime : bookedEnd;
		}

		// 마지막 예약 이후 빈 공간 추가
		if (currentTime.isBefore(operationEnd)) {
			availableSlots.add(new TimeSlot(currentTime, operationEnd));
		}

		return availableSlots;
	}

	/**
	 * 날짜 유효성 검증
	 * @throws InvalidInputValueException 유효하지 않은 날짜
	 */
	private LocalDate validateAndGetDate(int year, int month, int day) {
		try {
			return LocalDate.of(year, month, day);
		} catch (DateTimeException e) {
			throw new InvalidInputValueException("유효하지 않은 날짜입니다: " + year + "-" + month + "-" + day);
		}
	}
}
