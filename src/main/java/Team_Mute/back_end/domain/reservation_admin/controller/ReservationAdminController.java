package Team_Mute.back_end.domain.reservation_admin.controller;

import Team_Mute.back_end.domain.reservation_admin.dto.request.RejectRequestDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ApproveResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.RejectResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.service.ReservationAdminService;
import Team_Mute.back_end.domain.space_admin.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/reservations-admin")
public class ReservationAdminController {
	private final ReservationAdminService reservationAdminService;

	public ReservationAdminController(ReservationAdminService reservationService) {
		this.reservationAdminService = reservationService;
	}

	// 1차 승인
	@PostMapping("/approve/first/{reservationId}")
	@Operation(summary = "1차 승인", description = "토큰을 확인하여 1차 승인을 진행합니다.")
	public ResponseEntity<ApproveResponseDto> approveFirst(Authentication authentication, @PathVariable Long reservationId) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());

		return ResponseEntity.ok(reservationAdminService.approveFirst(adminId, reservationId));
	}

	// 2차 승인
	@PostMapping("/approve/second/{reservationId}")
	@Operation(summary = "2차 승인", description = "토큰을 확인하여 2차 승인을 진행합니다.")
	public ResponseEntity<ApproveResponseDto> approveSecond(Authentication authentication, @PathVariable Long reservationId) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());

		return ResponseEntity.ok(reservationAdminService.approveSecond(adminId, reservationId));
	}

	// 예약 반려
	@PostMapping(value = "/reject/{reservationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "예약 반려", description = "토큰을 확인하여 반려 진행합니다.")
	public ResponseEntity<RejectResponseDto> rejectReservation(
		Authentication authentication,
		@PathVariable Long reservationId,
		@org.springframework.web.bind.annotation.RequestBody RejectRequestDto requestDto) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());

		RejectResponseDto response = reservationAdminService.rejectReservation(
			adminId,
			reservationId,
			requestDto
		);

		return ResponseEntity.ok(response);
	}

	// 예약 리스트 조회
	@GetMapping("/list")
	@Operation(summary = "예약 리스트 조회", description = "토큰을 확인하여 예약 리스트를 조회합니다.")
	public ResponseEntity<PagedResponse<ReservationListResponseDto>> getAllReservations(
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "5") int size
	) {
		Pageable pageable = PageRequest.of(
			Math.max(page - 1, 0),
			size,
			Sort.by(Sort.Order.desc("regDate")) // 최신 등록 순
		);

		Page<ReservationListResponseDto> reservationPage = reservationAdminService.getAllReservations(pageable);
		PagedResponse<ReservationListResponseDto> response = new PagedResponse<>(reservationPage);
		return ResponseEntity.ok(response);
	}
}
