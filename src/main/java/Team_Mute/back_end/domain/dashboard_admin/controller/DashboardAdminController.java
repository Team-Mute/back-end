package Team_Mute.back_end.domain.dashboard_admin.controller;

import Team_Mute.back_end.domain.dashboard_admin.dto.response.CalendernFilterItemResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.dto.response.ReservationCalendarResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.dto.response.ReservationCountResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.dto.response.SelectItemResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.service.DashboardAdminService;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대시보드 관련 요청을 처리하는 컨트롤러입니다.
 * 예약 현황 카운트, 캘린더 리스트 조회, 특정 날짜별 예약 리스트 조회 기능을 제공
 */
@Validated
@Tag(name = "관리자 대시보드 API", description = "관리자 대시보드 관련 API 명세")
@RestController
@RequestMapping("/api/dashboard-admin")
public class DashboardAdminController {

	private final DashboardAdminService dashboardAdminService;

	/**
	 * DashboardAdminController의 생성자
	 *
	 * @param dashboardAdminService 대시보드 관련 비즈니스 로직을 처리하는 서비스
	 */
	public DashboardAdminController(
		DashboardAdminService dashboardAdminService
	) {
		this.dashboardAdminService = dashboardAdminService;
	}

	/**
	 * 대시보드 카드
	 * - 대시보드의 예약 현황 카운트 정보를 조회
	 * - 인증 토큰에서 관리자 ID를 추출하여 해당 관리자의 권한에 맞는 예약 건수를 반환
	 *
	 * @param authentication 현재 인증된 사용자의 정보 (Admin ID 포함)
	 * @return 예약 현황 요약 데이터 (1차 승인 대기, 2차 승인 대기, 긴급, 신한) 리스트를 포함하는 {@code ResponseEntity}
	 */
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
	 * 캘린더 커스터마이징 화면 구성을 위한 리스트 조회
	 */
	@GetMapping("/filters")
	@Operation(summary = "캘린더 설정 필터링 리스트 조회", description = "토큰을 확인하여 캘린더 설정 필터링 리스트를 조회합니다.")
	public ResponseEntity<List<CalendernFilterItemResponseDto>> getReservationFilterItemList() {
		List<CalendernFilterItemResponseDto> statusList = dashboardAdminService.getReservationFilterItemList();
		return ResponseEntity.ok(statusList);
	}

	/**
	 * 대시보드 캘린더 리스트 조회
	 * - 관리자 대시보드 캘린더에 표시할 전체 예약 리스트를 조회
	 * - 인증 토큰에서 관리자 ID를 추출하여 권한에 맞는 예약 목록을 캘린더 DTO 형태로 반환
	 *
	 * @param authentication 현재 인증된 사용자의 정보 (Admin ID 포함)
	 * @param year           조회 연도 (필수, 예: 2024)
	 * @param month          조회 월 (필수, 예: 5)
	 * @param statusIds      캘린더에 표시할 예약 상태 ID 리스트 (예: 1,2,3)
	 * @return 캘린더 표시에 최적화된 예약 리스트 DTO 목록을 포함하는 {@code ResponseEntity}
	 */
	@GetMapping("/list")
	@Operation(summary = "캘린더 리스트 조회(연,월/상태 필터링)", description = "토큰을 확인하여 캘린더 리스트를 조회합니다.")
	public ResponseEntity<List<ReservationCalendarResponseDto>> getAllReservations(
		Authentication authentication,
		@RequestParam(required = true) @Min(value = 1900, message = "연도는 1900년 이상이어야 합니다.") Integer year,
		@RequestParam(required = true) @Min(value = 1, message = "월은 1월 이상이어야 합니다.") @Max(value = 12, message = "월은 12월 이하여야 합니다.") Integer month,
		@RequestParam(required = false) List<Integer> statusIds, // 예약 상태
		@RequestParam(required = false) Boolean isShinhan, // 신한 예약 플래그
		@RequestParam(required = false) Boolean isEmergency   // 긴급 예약 플래그
	) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		// 서비스 계층으로 변경된 파라미터를 전달
		List<ReservationCalendarResponseDto> reservationList =
			dashboardAdminService.getAllReservations(adminId, year, month, statusIds, isShinhan, isEmergency);

		return ResponseEntity.ok(reservationList);
	}

	/**
	 * 특정 날짜 예약 리스트 조회
	 * - 특정 날짜에 해당하는 예약 리스트를 상세 정보와 함께 조회
	 * - 캘린더에서 특정 날짜를 선택했을 때 해당 날짜의 예약 목록을 조회할 때 사용
	 *
	 * @param authentication 현재 인증된 사용자의 정보 (Admin ID 포함)
	 * @param dateString     조회할 날짜를 나타내는 "YYYY-MM-DD" 형식의 문자열
	 * @return 특정 날짜에 예약된 상세 예약 리스트 DTO 목록을 포함하는 {@code ResponseEntity}
	 */
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
