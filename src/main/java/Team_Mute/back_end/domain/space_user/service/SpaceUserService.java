package Team_Mute.back_end.domain.space_user.service;


import Team_Mute.back_end.domain.reservation.dto.response.AvailableTimeResponse;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.service.ReservationScheduleService;
import Team_Mute.back_end.domain.space_user.dto.SpaceUserDtailResponseDto;
import Team_Mute.back_end.domain.space_user.dto.SpaceUserResponseDto;
import Team_Mute.back_end.domain.space_user.repository.SpaceUserRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

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


	public List<SpaceUserResponseDto> searchSpaces(
		Integer regionId,
		Integer categoryId,
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
			regionId, categoryId, safePeople, safeTags, tagCount
		);

		// 2. 날짜/시간 정보가 없으면 1차 필터링 결과 그대로 반환
		if (startDateTime == null || endDateTime == null) {
			return initialFilteredSpaces;
		}

		// 3. 날짜/시간 조건으로 2차 필터링
		List<SpaceUserResponseDto> finalAvailableSpaces = new ArrayList<>();
		int year = startDateTime.getYear();
		int month = startDateTime.getMonthValue();
		int day = startDateTime.getDayOfMonth();
		LocalTime startTime = startDateTime.toLocalTime();
		LocalTime endTime = endDateTime.toLocalTime();

		for (SpaceUserResponseDto space : initialFilteredSpaces) {
			try {
				// 예약 가능 시간 보여주는 getAvailableTimes() 메서드를 활용
				List<AvailableTimeResponse.TimeSlot> availableTimes = reservationScheduleService.getAvailableTimes(
					year,
					month,
					day,
					space.getSpaceId()
				);

				// 사용자가 선택한 시간대가 예약 가능한지 확인
				if (isTimeSlotAvailable(availableTimes, startTime, endTime)) {
					finalAvailableSpaces.add(space);
				}
			} catch (InvalidInputValueException e) {
				// 해당 날짜가 휴무일이거나 유효하지 않은 경우, 이 공간은 건너뜀
				continue;
			}
		}

		return finalAvailableSpaces;
	}

	// 주어진 예약 가능 시간 슬롯 목록에서 특정 시간대(from, to)가 사용 가능한지 확인하는 헬퍼 메서드
	private boolean isTimeSlotAvailable(List<AvailableTimeResponse.TimeSlot> availableTimes, LocalTime startTime, LocalTime endTime) {
		// 시작 시간이 종료 시간보다 뒤에 있으면 유효하지 않음
		if (startTime.isAfter(endTime)) return false;

		// 사용자가 선택한 시간대(startTime ~ endTime)가 예약 가능한 슬롯에 완전히 포함되는지 확인
		return availableTimes.stream()
			.anyMatch(slot ->
				!startTime.isBefore(slot.getStartTime()) && !endTime.isAfter(slot.getEndTime())
			);
	}

	// 특정 공간 상세 정보 조회
	public SpaceUserDtailResponseDto getSpaceById(Integer spaceId) {
		return spaceUserRepository.findSpaceDetail(spaceId)
			.orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
	}
}
