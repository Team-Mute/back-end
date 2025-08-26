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

import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.dto.response.AvailableTimeResponse.TimeSlot;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.repository.ReservationRepository;
import Team_Mute.back_end.domain.space_admin.entity.SpaceClosedDay;
import Team_Mute.back_end.domain.space_admin.entity.SpaceOperation;
import Team_Mute.back_end.domain.space_admin.repository.SpaceClosedDayRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceOperationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationScheduleService {

	private final SpaceOperationRepository spaceOperationRepository;
	private final SpaceClosedDayRepository spaceClosedDayRepository;
	private final ReservationRepository reservationRepository;

	public List<TimeSlot> getAvailableTimes(int year, int month, int day, int spaceId) {
		LocalDate requestedDate = validateAndGetDate(year, month, day);

		// 1. 해당 날짜가 운영일인지, 휴무일은 아닌지 확인
		if (!isBookableDay(requestedDate, spaceId)) {
			throw new InvalidInputValueException("선택하신 날짜는 운영일이 아니거나 휴무일입니다.");
		}

		// 2. 해당 날짜의 운영 시간 가져오기
		SpaceOperation operation = getOperationForDay(requestedDate, spaceId);
		if (operation == null) { // 이중 체크
			throw new InvalidInputValueException("해당 요일의 운영 정보를 찾을 수 없습니다.");
		}
		LocalTime operationStart = operation.getOperationFrom();
		LocalTime operationEnd = operation.getOperationTo();

		// 3. 해당 날짜의 모든 예약 시간(일반+사전답사) 목록 가져오기
		List<LocalTime[]> bookedSlots = getBookedSlotsForDay(requestedDate, spaceId);

		// 4. 운영 시간에서 예약된 시간을 제외하여 가능한 시간 슬롯 계산
		return calculateAvailableTimeSlots(operationStart, operationEnd, bookedSlots);
	}

	public List<Integer> getAvailableDays(int year, int month, int spaceId) {
		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
		LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

		// Step 1: 운영 요일 기반으로 가능한 모든 날짜 계산
		Set<Integer> openDays = getOpenDaysOfMonth(spaceId, yearMonth);

		// Step 2: 지정된 휴무일 제외
		removeClosedDays(openDays, spaceId, startOfMonth, endOfMonth);

		// Step 3: 예약이 꽉 찬 날짜 제외
		removeFullyBookedDays(openDays, spaceId, startOfMonth, endOfMonth, yearMonth);

		// 최종 결과를 정렬하여 반환
		return openDays.stream().sorted().collect(Collectors.toList());
	}

	// 1-3. 운영 요일 기반으로 해당 월의 가능한 날짜 목록 생성
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

	// 2-2. 휴무일 제외
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

	// 3-6. 예약이 꽉 찬 날짜 제외
	private void removeFullyBookedDays(Set<Integer> openDays, Integer spaceId, LocalDateTime start, LocalDateTime end,
		YearMonth yearMonth) {
		// 3-1. 유효한 예약 조회
		List<Long> validStatusIds = Arrays.asList(1L, 2L, 3L);
		List<Reservation> reservations = reservationRepository.findReservationsBySpaceAndMonth(spaceId, start, end,
			validStatusIds);

		// 3-3, 3-4. 일자별 예약 시간 집계 (예약 + 사전답사)
		Map<Integer, List<LocalTime[]>> dailyBookedTimes = new HashMap<>();
		for (Reservation r : reservations) {
			addBookedTimes(dailyBookedTimes, r.getReservationFrom(), r.getReservationTo(), yearMonth);
			// 3-2. 사전답사 조회
			for (PrevisitReservation pr : r.getPrevisitReservations()) {
				addBookedTimes(dailyBookedTimes, pr.getPrevisitFrom(), pr.getPrevisitTo(), yearMonth);
			}
		}

		// 3-5. 운영 시간 정보 가져오기
		Map<DayOfWeek, SpaceOperation> operationMap = spaceOperationRepository.findBySpace_SpaceId(spaceId).stream()
			.collect(Collectors.toMap(
				op -> DayOfWeek.of(op.getDay()), // op.getDay()가 1~7 사이의 값이라고 가정
				op -> op,
				(op1, op2) -> op1 // 중복 키 발생 시 첫 번째 값 사용 (혹은 로직에 맞게 수정)
			));

		// 예약이 꽉 찬 날짜를 식별하여 제거
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

	// 예약 시간(from, to)을 일자별로 맵에 추가하는 헬퍼 메서드
	private void addBookedTimes(Map<Integer, List<LocalTime[]>> map, LocalDateTime from, LocalDateTime to,
		YearMonth yearMonth) {
		IntStream.rangeClosed(from.getDayOfMonth(), to.getDayOfMonth()).forEach(day -> {
			// 해당 월에 속하는 날짜만 처리
			if (from.getYear() == yearMonth.getYear() && from.getMonth() == yearMonth.getMonth()) {
				LocalTime startTime = (day == from.getDayOfMonth()) ? from.toLocalTime() : LocalTime.MIN;
				LocalTime endTime = (day == to.getDayOfMonth()) ? to.toLocalTime() : LocalTime.MAX;
				map.computeIfAbsent(day, k -> new ArrayList<>()).add(new LocalTime[] {startTime, endTime});
			}
		});
	}

	// 중복 시간을 고려하여 총 예약된 시간을 계산하는 헬퍼 메서드
	private long calculateTotalBookedMinutes(List<LocalTime[]> slots) {
		if (slots.isEmpty())
			return 0;
		// 시작 시간 기준으로 정렬
		slots.sort(Comparator.comparing(a -> a[0]));

		long totalMinutes = 0;
		LocalTime mergedStart = slots.get(0)[0];
		LocalTime mergedEnd = slots.get(0)[1];

		for (int i = 1; i < slots.size(); i++) {
			LocalTime[] current = slots.get(i);
			if (current[0].isBefore(mergedEnd)) { // 시간이 겹치는 경우
				mergedEnd = mergedEnd.isAfter(current[1]) ? mergedEnd : current[1];
			} else { // 겹치지 않는 경우
				totalMinutes += Duration.between(mergedStart, mergedEnd).toMinutes();
				mergedStart = current[0];
				mergedEnd = current[1];
			}
		}
		totalMinutes += Duration.between(mergedStart, mergedEnd).toMinutes();
		return totalMinutes;
	}

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
				return false; // 휴무일 기간에 포함됨
			}
		}
		return true;
	}

	private List<LocalTime[]> getBookedSlotsForDay(LocalDate date, int spaceId) {
		LocalDateTime startOfDay = date.atStartOfDay();
		LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

		List<Long> validStatusIds = Arrays.asList(1L, 2L, 3L);
		List<Reservation> reservations = reservationRepository.findReservationsBySpaceAndMonth(spaceId, startOfDay,
			endOfDay, validStatusIds);

		List<LocalTime[]> bookedSlots = new ArrayList<>();
		for (Reservation r : reservations) {
			addBookedSlotForDay(bookedSlots, r.getReservationFrom(), r.getReservationTo(), date);
			for (PrevisitReservation pr : r.getPrevisitReservations()) {
				addBookedSlotForDay(bookedSlots, pr.getPrevisitFrom(), pr.getPrevisitTo(), date);
			}
		}
		return bookedSlots;
	}

	private Map<DayOfWeek, SpaceOperation> getOperationMap(int spaceId) {
		return spaceOperationRepository.findBySpace_SpaceId(spaceId).stream()
			.collect(Collectors.toMap(
				op -> DayOfWeek.of(op.getDay()), // Integer를 DayOfWeek으로 변환
				op -> op,
				(op1, op2) -> op1 // 중복 키(같은 요일) 발생 시 첫 번째 값을 사용
			));
	}

	private SpaceOperation getOperationForDay(LocalDate date, int spaceId) {
		// 이 부분은 DB를 한번 더 조회하는 대신, 위 getOperationMap을 호출하여 재사용할 수 있습니다.
		return getOperationMap(spaceId).get(date.getDayOfWeek());
	}

	// 특정 날짜에 대한 예약 시간 슬롯을 리스트에 추가하는 헬퍼 메서드
	private void addBookedSlotForDay(List<LocalTime[]> slots, LocalDateTime from, LocalDateTime to,
		LocalDate targetDate) {
		if (from.toLocalDate().isEqual(targetDate) || to.toLocalDate().isEqual(targetDate) ||
			(from.toLocalDate().isBefore(targetDate) && to.toLocalDate().isAfter(targetDate))) {

			LocalTime startTime = from.toLocalDate().isEqual(targetDate) ? from.toLocalTime() : LocalTime.MIN;
			LocalTime endTime = to.toLocalDate().isEqual(targetDate) ? to.toLocalTime() : LocalTime.MAX;
			slots.add(new LocalTime[] {startTime, endTime});
		}
	}

	// 최종적으로 예약 가능한 시간대를 계산하는 로직
	private List<TimeSlot> calculateAvailableTimeSlots(LocalTime operationStart, LocalTime operationEnd,
		List<LocalTime[]> bookedSlots) {
		List<TimeSlot> availableSlots = new ArrayList<>();

		// 예약된 시간을 시작 시간 기준으로 정렬
		bookedSlots.sort(Comparator.comparing(a -> a[0]));

		LocalTime currentTime = operationStart;

		for (LocalTime[] slot : bookedSlots) {
			LocalTime bookedStart = slot[0];
			LocalTime bookedEnd = slot[1];

			// 현재 시간과 예약 시작 시간 사이에 빈 공간이 있다면 추가
			if (currentTime.isBefore(bookedStart)) {
				availableSlots.add(new TimeSlot(currentTime, bookedStart));
			}
			// 현재 시간을 예약 종료 시간 이후로 이동
			currentTime = currentTime.isAfter(bookedEnd) ? currentTime : bookedEnd;
		}

		// 마지막 예약 시간 이후부터 운영 종료 시간까지 빈 공간이 있다면 추가
		if (currentTime.isBefore(operationEnd)) {
			availableSlots.add(new TimeSlot(currentTime, operationEnd));
		}

		return availableSlots;
	}

	// 요청 날짜 유효성 검사
	private LocalDate validateAndGetDate(int year, int month, int day) {
		try {
			return LocalDate.of(year, month, day);
		} catch (DateTimeException e) {
			throw new InvalidInputValueException("유효하지 않은 날짜입니다: " + year + "-" + month + "-" + day);
		}
	}
}
