package Team_Mute.back_end.domain.reservation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation.repository.ReservationRepository;
import Team_Mute.back_end.domain.space_admin.entity.SpaceClosedDay;
import Team_Mute.back_end.domain.space_admin.entity.SpaceOperation;
import Team_Mute.back_end.domain.space_admin.repository.SpaceClosedDayRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceOperationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private SpaceOperationRepository spaceOperationRepository;

	@MockitoBean
	private SpaceClosedDayRepository spaceClosedDayRepository;

	@MockitoBean
	private ReservationRepository reservationRepository;

	private final int SPACE_ID = 1;
	private final int YEAR = 2025;
	private final int MONTH = 10;

	@BeforeEach
	void setUp() {
		// 1. 운영 시간 Mocking: 월~금 09:00~18:00 운영, 주말 휴무
		List<SpaceOperation> operations = Arrays.asList(
			createOperation(DayOfWeek.MONDAY, true), createOperation(DayOfWeek.TUESDAY, true),
			createOperation(DayOfWeek.WEDNESDAY, true), createOperation(DayOfWeek.THURSDAY, true),
			createOperation(DayOfWeek.FRIDAY, true), createOperation(DayOfWeek.SATURDAY, false),
			createOperation(DayOfWeek.SUNDAY, false)
		);
		when(spaceOperationRepository.findBySpace_SpaceId(SPACE_ID)).thenReturn(operations);

		// 2. 휴무일 Mocking: 지정된 휴무일 없음
		SpaceClosedDay closedDay = new SpaceClosedDay();
		closedDay.setClosedFrom(LocalDateTime.of(YEAR, MONTH, 23, 0, 0));
		closedDay.setClosedTo(LocalDateTime.of(YEAR, MONTH, 23, 23, 59));

		when(spaceClosedDayRepository.findBySpaceIdAndMonth(anyInt(), any(LocalDateTime.class),
			any(LocalDateTime.class)))
			.thenReturn(Collections.singletonList(closedDay));

		// 3. 예약 데이터 Mocking
		// 10월 21일 (화) 예약: 11:00 ~ 13:00
		Reservation reservation1 = new Reservation();
		reservation1.setReservationFrom(LocalDateTime.of(YEAR, MONTH, 21, 11, 0));
		reservation1.setReservationTo(LocalDateTime.of(YEAR, MONTH, 21, 13, 0));
		reservation1.setReservationStatus(new ReservationStatus(1L, "예약확정")); // statusId=1

		// 10월 22일 (수) 예약: 09:00 ~ 18:00 (꽉 찬 날)
		Reservation reservation2 = new Reservation();
		reservation2.setReservationFrom(LocalDateTime.of(YEAR, MONTH, 22, 9, 0));
		reservation2.setReservationTo(LocalDateTime.of(YEAR, MONTH, 22, 18, 0));
		reservation2.setReservationStatus(new ReservationStatus(1L, "예약확정"));

		when(reservationRepository.findReservationsBySpaceAndMonth(anyInt(), any(LocalDateTime.class),
			any(LocalDateTime.class), anyList()))
			.thenReturn(Arrays.asList(reservation1, reservation2));
	}

	private SpaceOperation createOperation(DayOfWeek day, boolean isOpen) {
		SpaceOperation op = new SpaceOperation();
		op.setDay(day.getValue());
		op.setIsOpen(isOpen);
		if (isOpen) {
			op.setOperationFrom(LocalTime.of(9, 0));
			op.setOperationTo(LocalTime.of(18, 0));
		}
		return op;
	}

	@Test
	@DisplayName("특정 월의 예약 가능일 조회 시, 예약 꽉 찬 날과 주말은 제외되어야 한다")
	void getAvailableDates_shouldExcludeFullyBookedAndClosedDays() throws Exception {
		// given
		String content = String.format("{\"spaceId\": %d, \"year\": %d, \"month\": %d}", SPACE_ID, YEAR, MONTH);

		// when & then
		mockMvc.perform(post("/api/reservations/available-dates")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableDays").isArray())
			.andExpect(jsonPath("$.availableDays[?(@ == 20)]").exists()) // 월요일 (예약 없음)
			.andExpect(jsonPath("$.availableDays[?(@ == 21)]").exists()) // 화요일 (일부 예약)
			.andExpect(jsonPath("$.availableDays[?(@ == 22)]").doesNotExist()) // 수요일 (꽉 참)
			.andExpect(jsonPath("$.availableDays[?(@ == 23)]").doesNotExist())
			.andExpect(jsonPath("$.availableDays[?(@ == 25)]").doesNotExist()); // 토요일 (주말)
	}

	@Test
	@DisplayName("예약 없는 날의 가능 시간 조회 시, 전체 운영 시간이 반환되어야 한다")
	void getAvailableTimes_onEmptyDay_shouldReturnFullOperationTime() throws Exception {
		// given
		String content = String.format("{\"spaceId\": %d, \"year\": %d, \"month\": %d, \"day\": 20}", SPACE_ID, YEAR,
			MONTH);

		// when: 10월 20일에 대한 예약은 없다고 가정 (setUp에서 설정된 예약 외 다른 날짜)
		when(reservationRepository.findReservationsBySpaceAndMonth(anyInt(), any(LocalDateTime.class),
			any(LocalDateTime.class), anyList()))
			.thenReturn(Collections.emptyList());

		// then
		mockMvc.perform(post("/api/reservations/available-times")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableTimes.length()").value(1))
			.andExpect(jsonPath("$.availableTimes[0].startTime").value("09:00:00"))
			.andExpect(jsonPath("$.availableTimes[0].endTime").value("18:00:00"));
	}

	@Test
	@DisplayName("일부 예약 있는 날의 가능 시간 조회 시, 예약된 시간은 제외되어야 한다")
	void getAvailableTimes_onPartiallyBookedDay_shouldExcludeBookedSlots() throws Exception {
		// given
		String content = String.format("{\"spaceId\": %d, \"year\": %d, \"month\": %d, \"day\": 21}", SPACE_ID, YEAR,
			MONTH);

		// when & then
		mockMvc.perform(post("/api/reservations/available-times")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableTimes.length()").value(2))
			.andExpect(jsonPath("$.availableTimes[0].startTime").value("09:00:00"))
			.andExpect(jsonPath("$.availableTimes[0].endTime").value("11:00:00"))
			.andExpect(jsonPath("$.availableTimes[1].startTime").value("13:00:00"))
			.andExpect(jsonPath("$.availableTimes[1].endTime").value("18:00:00"));
	}

	@Test
	@DisplayName("운영하지 않는 날의 가능 시간 조회 시, 400 에러와 메시지를 반환해야 한다")
	void getAvailableTimes_onClosedDay_shouldReturnBadRequest() throws Exception {
		// given
		String content = String.format("{\"spaceId\": %d, \"year\": %d, \"month\": %d, \"day\": 25}", SPACE_ID, YEAR,
			MONTH);

		// when & then
		mockMvc.perform(post("/api/reservations/available-times")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("선택하신 날짜는 운영일이 아니거나 휴무일입니다."));
	}

	@Test
	@DisplayName("지정된 휴무일의 가능 시간 조회 시, 400 에러와 메시지를 반환해야 한다")
	void getAvailableTimes_onDesignatedClosedDay_shouldReturnBadRequest() throws Exception {
		// given
		// 10월 23일은 setUp()에서 휴무일로 설정되었음
		String content = String.format("{\"spaceId\": %d, \"year\": %d, \"month\": %d, \"day\": 23}", SPACE_ID, YEAR,
			MONTH);

		// when & then
		mockMvc.perform(post("/api/reservations/available-times")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("선택하신 날짜는 운영일이 아니거나 휴무일입니다."));
	}
}
