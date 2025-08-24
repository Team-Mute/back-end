package Team_Mute.back_end.domain.reservation_admin.controller;

import Team_Mute.back_end.domain.reservation_admin.dto.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.service.ReservationAdminService;
import Team_Mute.back_end.domain.space_admin.dto.PagedResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations-admin")
public class ReservationAdminController {
	private final ReservationAdminService reservationAdminService;

	public ReservationAdminController(ReservationAdminService reservationService) {
		this.reservationAdminService = reservationService;
	}

	@GetMapping("/list")
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
