package Team_Mute.back_end.domain.dashboard_admin.controller;

import Team_Mute.back_end.domain.dashboard_admin.dto.ReservationCountResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.dto.SelectItemResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.service.DashboardAdminService;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.service.ReservationAdminService;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	// 대시보드 카드
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

	// 예약 리스트 조회
	@GetMapping("/list")
	@Operation(summary = "예약 리스트 조회", description = "토큰을 확인하여 예약 리스트를 조회합니다.")
	public ResponseEntity<List<ReservationListResponseDto>> getAllReservations(
		Authentication authentication
	) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		List<ReservationListResponseDto> reservationList = dashboardAdminService.getAllReservations(adminId);

		return ResponseEntity.ok(reservationList);
	}
}
