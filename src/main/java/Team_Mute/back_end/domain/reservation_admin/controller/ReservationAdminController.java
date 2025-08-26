package Team_Mute.back_end.domain.reservation_admin.controller;

import Team_Mute.back_end.domain.reservation_admin.dto.request.BulkApproveRequestDto;
import Team_Mute.back_end.domain.reservation_admin.dto.request.RejectRequestDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.BulkApproveResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.RejectResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationDetailResponseDto;
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

	// 다중 승인 에러 처리
	private org.springframework.http.HttpStatus resolveBulkStatus(BulkApproveResponseDto resp) {
		if (resp.getFailureCount() == 0) {
			return org.springframework.http.HttpStatus.OK; // 전부 성공
		}

		if (resp.getSuccessCount() == 0) { // 전부 실패
			boolean hasForbidden = resp.getResults().stream()
				.anyMatch(r -> !r.isSuccess() && "승인 권한이 없습니다.".equals(r.getMessage()));
			boolean hasNotFound = resp.getResults().stream()
				.anyMatch(r -> !r.isSuccess() && r.getMessage() != null &&
					(r.getMessage().contains("not found") || r.getMessage().contains("존재하지")));
			boolean hasStateConflict = resp.getResults().stream()
				.anyMatch(r -> !r.isSuccess() && r.getMessage() != null &&
					(r.getMessage().contains("이미 처리 완료") || r.getMessage().contains("상태")));

			if (hasForbidden) return org.springframework.http.HttpStatus.FORBIDDEN;      // 403 권한 문제
			if (hasNotFound) return org.springframework.http.HttpStatus.NOT_FOUND;      // 404 리소스 없음
			if (hasStateConflict) return org.springframework.http.HttpStatus.CONFLICT;     // 409 상태/전이 충돌
			return org.springframework.http.HttpStatus.BAD_REQUEST;                         // 400 그 외 전부 실패
		}

		// 일부 성공 일부 실패 → 부분 성공
		return org.springframework.http.HttpStatus.MULTI_STATUS; // 207
	}

	public ReservationAdminController(ReservationAdminService reservationService) {
		this.reservationAdminService = reservationService;
	}

	// 1차 승인
	@PostMapping("/approve/first")
	@Operation(summary = "1차 승인(단건 or 일괄)", description = "토큰을 확인하여 1차 승인을 진행합니다.")
	public ResponseEntity<BulkApproveResponseDto> approveFirstBulk(
		Authentication authentication,
		@org.springframework.web.bind.annotation.RequestBody BulkApproveRequestDto request
	) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		BulkApproveResponseDto resp = reservationAdminService.approveFirstBulk(adminId, request.getReservationIds());

		return ResponseEntity.status(resolveBulkStatus(resp)).body(resp);
	}

	// 2차 승인
	@PostMapping("/approve/second")
	@Operation(summary = "2차 승인(단건 or 일괄)", description = "토큰을 확인하여 2차 승인을 진행합니다.")
	public ResponseEntity<BulkApproveResponseDto> approveSecondBulk(
		Authentication authentication,
		@org.springframework.web.bind.annotation.RequestBody BulkApproveRequestDto request
	) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		BulkApproveResponseDto resp = reservationAdminService.approveSecondBulk(adminId, request.getReservationIds());

		return ResponseEntity.status(resolveBulkStatus(resp)).body(resp);
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
		Authentication authentication,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "5") int size
	) {
		Pageable pageable = PageRequest.of(
			Math.max(page - 1, 0),
			size,
			Sort.by(Sort.Order.desc("regDate")) // 최신 등록 순
		);
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		Page<ReservationListResponseDto> reservationPage = reservationAdminService.getAllReservations(adminId, pageable);
		PagedResponse<ReservationListResponseDto> response = new PagedResponse<>(reservationPage);
		return ResponseEntity.ok(response);
	}

	// 예약 상세 조회
	@GetMapping("/detail/{reservationId}")
	@Operation(summary = "예약 상세 조회", description = "토큰을 확인하여 예약 상세를 조회합니다.")
	public ResponseEntity<ReservationDetailResponseDto> getById(@PathVariable Long reservationId) {
		return ResponseEntity.ok(reservationAdminService.getByReservationId(reservationId));
	}
}
