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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SpaceUserService {
	private final SpaceUserRepository spaceUserRepository;
	private final ReservationScheduleService reservationScheduleService;

	public SpaceUserService(
		SpaceUserRepository spaceUserRepository,
		ReservationScheduleService reservationScheduleService
	) {
		this.spaceUserRepository = spaceUserRepository;
		this.reservationScheduleService = reservationScheduleService;
	}

	public SpaceSearchResponse searchSpaces(
		Integer regionId,
		Integer people,
		String[] tagNames,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime
	) {
		// 1. 다른 조건들로 1차 필터링
		Integer safePeople = (people == null || people <= 0) ? null : people;
		int tagCount = (tagNames == null) ? 0 : tagNames.length;
		String[] safeTags = (tagCount == 0) ? new String[]{} : tagNames;

		List<SpaceUserResponseDto> initialFilteredSpaces = spaceUserRepository.searchSpacesForUser(
			regionId, safePeople, safeTags, tagCount
		);

		// 2. 날짜/시간 조건으로 2차 필터링
		List<SpaceUserResponseDto> finalAvailableSpaces = new ArrayList<>();

		// 미팅룸, 행사장 리스트 생성
		List<SpaceUserResponseDto> meetingRoomFilteredSpaces = new ArrayList<>();
		List<SpaceUserResponseDto> eventHallFilteredSpaces = new ArrayList<>();

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

		// 사용자가 하루 전체 시간(00:00 ~ 23:59:59)을 요청했을 경우
		LocalTime requestedStartTime = startDateTime.toLocalTime();
		LocalTime requestedEndTime = endDateTime.toLocalTime();

		boolean isFullDayRequest = requestedStartTime.equals(LocalTime.MIN) &&
			(requestedEndTime.equals(LocalTime.MAX) || requestedEndTime.equals(LocalTime.of(23, 59, 59)));

		for (SpaceUserResponseDto space : initialFilteredSpaces) {
			try {
				List<AvailableTimeResponse.TimeSlot> availableTimeSlots = reservationScheduleService.getAvailableTimes(
					startDateTime.getYear(),
					startDateTime.getMonthValue(),
					startDateTime.getDayOfMonth(),
					space.getSpaceId()
				);

				boolean isAvailable = false;
				// 하루 종일 요청인 경우, 슬롯 목록이 비어있지 않은지만 확인
				if (isFullDayRequest) {
					isAvailable = availableTimeSlots != null && !availableTimeSlots.isEmpty();
				} else {
					// 특정 시간대 요청인 경우, 상세 가용성 확인
					isAvailable = this.isTimeSlotFullyAvailable(availableTimeSlots, requestedStartTime, requestedEndTime);
				}

				if (isAvailable) {
					// 가용성 확인 후, 카테고리별로 분리하여 추가
					if (space.getCategoryId() == 1) { // 미팅룸
						meetingRoomFilteredSpaces.add(space);
					} else if (space.getCategoryId() == 2) { // 이벤트홀
						eventHallFilteredSpaces.add(space);
					}
				}
			} catch (InvalidInputValueException e) {
				// 선택하신 날짜가 운영일이 아니거나 휴무일일 경우, 에러 대신 건너뛰고 빈 배열을 반환
				continue;
			} catch (NullPointerException | DateTimeException e) {
				throw new InvalidInputValueException("유효하지 않은 예약 시간 정보입니다. 다시 확인해 주세요.");
			}
		}

		return SpaceSearchResponse.builder()
			.meetingRoom(meetingRoomFilteredSpaces)
			.eventHall(eventHallFilteredSpaces)
			.build();
	}

	// 주어진 예약 가능 시간 슬롯 목록에서 특정 시간대(from, to)가 사용 가능한지 확인하는 헬퍼 메서드
	private boolean isTimeSlotFullyAvailable(List<AvailableTimeResponse.TimeSlot> availableTimes, LocalTime startTime, LocalTime endTime) {
		// 1. 요청 시간이 유효한지 확인
		if (startTime == null || endTime == null || startTime.isAfter(endTime) || startTime.equals(endTime)) {
			return false;
		}

		// 2. 예약 가능 슬롯 목록이 비어있는지 확인
		if (availableTimes == null || availableTimes.isEmpty()) {
			return false;
		}

		// 3. 모든 예약 가능 슬롯을 순회하며 요청 시간대가 완전히 포함되는지 확인
		for (AvailableTimeResponse.TimeSlot slot : availableTimes) {
			if (!startTime.isBefore(slot.getStartTime()) && !endTime.isAfter(slot.getEndTime())) {
				return true; // 요청 시간이 이 슬롯 안에 완전히 포함됨
			}
		}

		return false;
	}

	// 특정 공간 상세 정보 조회
	public SpaceUserDtailResponseDto getSpaceById(Integer spaceId) {
		return spaceUserRepository.findSpaceDetail(spaceId)
			.orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
	}
}
