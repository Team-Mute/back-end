package Team_Mute.back_end.domain.dashboard_admin.controller;

import Team_Mute.back_end.domain.dashboard_admin.dto.ReservationCalendarResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.dto.ReservationCountResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.dto.SelectItemResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.service.DashboardAdminService;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.service.ReservationAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 대시보드 API", description = "관리자 대시보드 관련 API 명세")
@RestController
@RequestMapping("/api/dashboard-admin")
public class DashboardAdminController {

	private final DashboardAdminService dashboardAdminService;
	private final ReservationAdminService reservationAdminService;

	public DashboardAdminController(
		DashboardAdminService dashboardAdminService,
		ReservationAdminService reservationAdminService
	) {
		this.dashboardAdminService = dashboardAdminService;
		this.reservationAdminService = reservationAdminService;
	}

	/**
	 * 대시보드 카드
	 **/
	@GetMapping("/counts")
	@Operation(summary = "예약 현황 카운트 조회", description = "각 필터링 조건에 맞는 예약 건수를 조회합니다.")
	public ResponseEntity<List<SelectItemResponseDto>> getReservationCounts(Authentication authentication) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		ReservationCountResponseDto countsData = dashboardAdminService.getReservationCounts(adminId);

		List<SelectItemResponseDto> summaryData = Arrays.asList(
			new SelectItemResponseDto("1차 승인 대기", countsData.getWaitingFistApprovalCount()),
			new SelectItemResponseDto("2차 승인 대기", countsData.getWaitingSecondApprovalCount()),
			new SelectItemResponseDto("긴급", countsData.getEmergency()),
			new SelectItemResponseDto("신한", countsData.getShinhan())
		);

		return ResponseEntity.ok(summaryData);
	}

	/**
	 * 대시보드 캘린더 리스트 조회
	 **/
	@GetMapping("/list")
	@Operation(summary = "캘린더 리스트 조회", description = "토큰을 확인하여 캘린더 리스트를 조회합니다.")
	public ResponseEntity<List<ReservationCalendarResponseDto>> getAllReservations(
		Authentication authentication
	) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		List<ReservationCalendarResponseDto> reservationList = dashboardAdminService.getAllReservations(adminId);

		return ResponseEntity.ok(reservationList);
	}

	/**
	 * 특정 날짜 예약 리스트 조회
	 **/
	@GetMapping("/list/by-date")
	@Operation(summary = "특정 날짜 예약 리스트 조회", description = "토큰을 확인하여 선택된 날짜(캘린더)의 예약 리스트를 조회합니다.")
	public ResponseEntity<List<ReservationListResponseDto>> getReservationsByDate(
		Authentication authentication,
		@RequestParam("date") String dateString
	) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		LocalDate date = LocalDate.parse(dateString); // "YYYY-MM-DD" 형식의 문자열을 LocalDate로 변환
		List<ReservationListResponseDto> reservationList = dashboardAdminService.getReservationsByDate(adminId, date);

		return ResponseEntity.ok(reservationList);
	}
}
