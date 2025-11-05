package Team_Mute.back_end.domain.space_user.service;


import Team_Mute.back_end.domain.reservation.dto.response.AvailableTimeResponse;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.service.ReservationScheduleService;
import Team_Mute.back_end.domain.space_user.dto.SpaceSearchResponse;
import Team_Mute.back_end.domain.space_user.dto.SpaceUserDtailResponseDto;
import Team_Mute.back_end.domain.space_user.dto.SpaceUserResponseDto;
import Team_Mute.back_end.domain.space_user.repository.SpaceUserRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * 사용자 공간 검색 및 조회 관련 비즈니스 로직을 처리하는 서비스 클래스
 */
@Slf4j
@Service
public class SpaceUserService {
	private final SpaceUserRepository spaceUserRepository;
	private final ReservationScheduleService reservationScheduleService;

	// END_OF_DAY_TIME 선언: 23시 59분 59초로 명시적 상수를 정의합니다.
	private static final LocalTime END_OF_DAY_TIME = LocalTime.of(23, 59, 59);

	/**
	 * SpaceUserService의 생성자
	 *
	 * @param spaceUserRepository        공간 데이터 접근을 위한 레포지토리
	 * @param reservationScheduleService 예약 가능 스케줄 확인 로직을 담당하는 서비스
	 */
	public SpaceUserService(
		SpaceUserRepository spaceUserRepository,
		ReservationScheduleService reservationScheduleService
	) {
		this.spaceUserRepository = spaceUserRepository;
		this.reservationScheduleService = reservationScheduleService;
	}

	/**
	 * 주어진 기간(start ~ end) 동안 해당 공간이 완전히 예약 가능한지 확인합니다.
	 * 기간 검색 시나리오를 처리하는 핵심 메서드입니다.
	 *
	 * @param startDateTime 검색 시작 시간
	 * @param endDateTime   검색 종료 시간
	 * @param spaceId       공간 ID
	 * @return 기간 내내 해당 공간이 예약 가능하면 true, 아니면 false
	 */
	public boolean isSpaceAvailableInPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime, int spaceId) {
		LocalDate startDate = startDateTime.toLocalDate();
		LocalDate endDate = endDateTime.toLocalDate();
		LocalTime requestedStartTime = startDateTime.toLocalTime();
		LocalTime requestedEndTime = endDateTime.toLocalTime();

		// 기간 내 모든 날짜를 순회하며 가용성 확인
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

			LocalTime searchStartTime = (date.isEqual(startDate)) ? requestedStartTime : LocalTime.MIN;
			LocalTime searchEndTime;
			if (date.isEqual(endDate)) {
				searchEndTime = requestedEndTime;
			} else {
				searchEndTime = END_OF_DAY_TIME; // 시간 경계 통일 (23:59:59)
			}

			// 해당 날짜의 가용 시간 슬롯 가져오기
			List<AvailableTimeResponse.TimeSlot> availableTimeSlots;
			try {
				availableTimeSlots = reservationScheduleService.getAvailableTimes(
					date.getYear(),
					date.getMonthValue(),
					date.getDayOfMonth(),
					spaceId
				);
			} catch (InvalidInputValueException e) {
				return false;
			}

			// 기간 검색의 검색 범위 정규화 (운영 시간 계산이 아닌, 기술적 한계 회피를 위한 경계값 조정)
			if (!availableTimeSlots.isEmpty()) {
				// 1) 시작 시간 조정: 00:00:00 요청 시 실제 운영 시작 시간으로 변경
				if (searchStartTime.equals(LocalTime.MIN)) {
					searchStartTime = availableTimeSlots.get(0).getStartTime();
				}

				// 2) 종료 시간 조정: 23:59:59 요청 시 실제 운영 종료 시간으로 변경
				if (searchEndTime.equals(END_OF_DAY_TIME)) {
					LocalTime operatingEndTime = availableTimeSlots.get(availableTimeSlots.size() - 1).getEndTime();
					searchEndTime = operatingEndTime;
				}
			}

			// 해당 날짜의 요청된 시간 범위(searchStartTime ~ searchEndTime)가 가용 슬롯에 완전히 포함되는지 검증
			if (!isTimeSlotFullyAvailable(availableTimeSlots, searchStartTime, searchEndTime)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 주어진 가용 시간 슬롯 목록 내에서 특정 요청 시간대(start ~ end)가 완전히 예약 가능한지 확인합니다.
	 * <p>
	 * 이 메서드는 요청된 시작 시간(start)이 어떤 가용 슬롯의 시작 시간보다 늦지 않고,
	 * 요청된 종료 시간(end)이 해당 가용 슬롯의 종료 시간보다 빠르지 않은 경우에만 'true'를 반환합니다.
	 * 즉, 요청된 시간대가 가용 슬롯의 경계 안에 완전히 들어가야만 예약이 가능하다고 판단합니다.
	 *
	 * @param availableTimeSlots ReservationScheduleService에서 계산된 가용 시간 슬롯 목록
	 * @param start              사용자가 요청한 예약 시작 시간 (LocalTime)
	 * @param end                사용자가 요청한 예약 종료 시간 (LocalTime)
	 * @return 요청 시간대가 하나의 가용 슬롯에 완전히 포함되면 true, 아니면 false
	 */
	private boolean isTimeSlotFullyAvailable(List<AvailableTimeResponse.TimeSlot> availableTimeSlots, LocalTime start, LocalTime end) {
		if (start.isAfter(end) || start.equals(end)) {
			return false; // 유효하지 않은 요청 시간
		}

		// 요청 시간을 나노초가 0인 상태로 정규화합니다.
		LocalTime normalizedStart = start.withNano(0);
		LocalTime normalizedEnd = end.withNano(0);

		for (AvailableTimeResponse.TimeSlot slot : availableTimeSlots) {
			// 가용 슬롯 시간도 나노초를 0으로 정규화하여 비교
			LocalTime slotStart = slot.getStartTime().withNano(0);
			LocalTime slotEnd = slot.getEndTime().withNano(0);

			// 요청된 시간대가 가용 슬롯 내부에 완전히 포함되는지 확인
			// (정규화된 시간을 사용하여 비교)
			if (!slotStart.isAfter(normalizedStart) && !slotEnd.isBefore(normalizedEnd)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 사용자 요청 조건에 따라 공간을 검색하고 분류
	 * 1차적으로 지역/인원/태그로 필터링한 후, 예약 시간 조건이 있을 경우 예약 스케줄 서비스를 통해 필터링
	 *
	 * @param regionId      지역 ID
	 * @param people        최소 인원
	 * @param tagNames      편의시설 태그 목록
	 * @param startDateTime 예약 시작 시간 (ISO 8601 형식, 시간 필터링 조건)
	 * @param endDateTime   예약 종료 시간 (ISO 8601 형식, 시간 필터링 조건)
	 * @return 카테고리별(미팅룸/이벤트홀)로 분류된 검색 결과를 담은 {@code SpaceSearchResponse}
	 * @throws InvalidInputValueException 시간 정보가 유효하지 않을 경우
	 */
	public SpaceSearchResponse searchSpaces(
		Integer regionId,
		Integer people,
		String[] tagNames,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime
	) {
		// 다른 조건들로 1차 필터링
		Integer safePeople = (people == null || people <= 0) ? null : people;
		int tagCount = (tagNames == null) ? 0 : tagNames.length;
		String[] safeTags = (tagCount == 0) ? new String[]{} : tagNames;

		List<SpaceUserResponseDto> initialFilteredSpaces = spaceUserRepository.searchSpacesForUser(
			regionId, safePeople, safeTags, tagCount
		);

		// 미팅룸, 행사장 리스트 생성
		List<SpaceUserResponseDto> meetingRoomFilteredSpaces = new ArrayList<>();
		List<SpaceUserResponseDto> eventHallFilteredSpaces = new ArrayList<>();

		// 2. 날짜/시간 조건으로 2차 필터링
		// 요청된 날짜/시간 값이 없을 경우, 시간 필터링을 건너뛰고 1차 필터링된 모든 공간을 반환
		if (startDateTime == null || endDateTime == null) {
			// 결과를 카테고리(1, 2)별로 분리
			for (SpaceUserResponseDto space : initialFilteredSpaces) {
				if (space.getCategoryId() == 1) {
					meetingRoomFilteredSpaces.add(space);
				} else if (space.getCategoryId() == 2) {
					eventHallFilteredSpaces.add(space);
				}
			}

			// SpaceSearchResponse DTO로 반환
			return SpaceSearchResponse.builder()
				.meetingRoom(meetingRoomFilteredSpaces)
				.eventHall(eventHallFilteredSpaces)
				.build();
		}

		// 시간 정보가 모두 있지만, 시작 시간이 종료 시간보다 늦은 경우
		if (startDateTime.isAfter(endDateTime) || startDateTime.isEqual(endDateTime)) {
			throw new InvalidInputValueException("예약 시작 시간은 종료 시간보다 빨라야 합니다.");
		}

		// 하루 검색인지, 기간 검색인지 판단
		boolean isMultiDayRequest = !startDateTime.toLocalDate().isEqual(endDateTime.toLocalDate());

		// 하루 검색 로직에서 사용되던 변수들
		LocalTime requestedStartTime = startDateTime.toLocalTime();
		LocalTime requestedEndTime = endDateTime.toLocalTime();
		boolean isFullDayRequest = requestedStartTime.equals(LocalTime.MIN) &&
			(requestedEndTime.equals(LocalTime.MAX) || requestedEndTime.equals(LocalTime.of(23, 59, 59)));

		// 기간 검색 + 하루 검색 처리
		for (SpaceUserResponseDto space : initialFilteredSpaces) {
			boolean isAvailable = false;

			try {
				if (isMultiDayRequest) {
					// 기간 검색 로직
					isAvailable = isSpaceAvailableInPeriod(
						startDateTime,
						endDateTime,
						space.getSpaceId()
					);
				} else {
					// 단일 날짜 검색 로직: 운영 시간 조정 로직이 여기에서 실행됩니다.
					List<AvailableTimeResponse.TimeSlot> availableTimeSlots = reservationScheduleService.getAvailableTimes(
						startDateTime.getYear(),
						startDateTime.getMonthValue(),
						startDateTime.getDayOfMonth(),
						space.getSpaceId()
					);

					LocalTime currentSearchStartTime = requestedStartTime;
					LocalTime currentSearchEndTime = requestedEndTime;

					// 요청 시간이 00:00:00 또는 23:59:59인 경우에만 실제 운영 시간으로 조정
					if (!availableTimeSlots.isEmpty()) {

						// 요청 시작 시간이 00:00:00이면 실제 운영 시작 시간으로 조정
						if (requestedStartTime.equals(LocalTime.MIN)) {
							currentSearchStartTime = availableTimeSlots.get(0).getStartTime();
						}

						// 요청 종료 시간이 23:59:59 (또는 MAX)이면 실제 운영 종료 시간으로 조정
						if (requestedEndTime.equals(LocalTime.MAX) || requestedEndTime.equals(LocalTime.of(23, 59, 59))) {
							LocalTime operatingEndTime = availableTimeSlots.get(availableTimeSlots.size() - 1).getEndTime();
							currentSearchEndTime = operatingEndTime;
						}
					}

					isAvailable = isTimeSlotFullyAvailable(
						availableTimeSlots,
						currentSearchStartTime, // 조정된 시간 사용
						currentSearchEndTime    // 조정된 시간 사용
					);
				}
			} catch (InvalidInputValueException e) {
				// 휴무일이거나 운영일이 아닌 경우, 에러 대신 건너뛰고 빈 배열을 반환
				continue;
			} catch (NullPointerException | DateTimeException e) {
				// 기타 시간 처리 오류
				throw new InvalidInputValueException("유효하지 않은 예약 시간 정보입니다. 다시 확인해 주세요.");
			}

			if (isAvailable) {
				// 가용성 확인 후, 카테고리별로 분리하여 추가
				if (space.getCategoryId() == 1) { // 미팅룸
					meetingRoomFilteredSpaces.add(space);
				} else if (space.getCategoryId() == 2) { // 이벤트홀
					eventHallFilteredSpaces.add(space);
				}
			}
		}

		return SpaceSearchResponse.builder()
			.meetingRoom(meetingRoomFilteredSpaces)
			.eventHall(eventHallFilteredSpaces)
			.build();
	}

	/**
	 * 특정 공간의 상세 정보를 조회
	 *
	 * @param spaceId 조회할 공간의 ID
	 * @return 공간 상세 정보를 담은 {@code SpaceUserDtailResponseDto}
	 * @throws NoSuchElementException 해당 ID의 공간을 찾을 수 없을 경우
	 */
	public SpaceUserDtailResponseDto getSpaceById(Integer spaceId) {
		return spaceUserRepository.findSpaceDetail(spaceId)
			.orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
	}
}
