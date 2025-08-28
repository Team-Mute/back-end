package Team_Mute.back_end.domain.reservation.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import Team_Mute.back_end.domain.reservation.dto.request.AvailableDateRequest;
import Team_Mute.back_end.domain.reservation.dto.request.AvailableTimeRequest;
import Team_Mute.back_end.domain.reservation.dto.request.ReservationRequestDto;
import Team_Mute.back_end.domain.reservation.dto.response.AvailableDateResponse;
import Team_Mute.back_end.domain.reservation.dto.response.AvailableTimeResponse;
import Team_Mute.back_end.domain.reservation.dto.response.PagedReservationResponse;
import Team_Mute.back_end.domain.reservation.dto.response.RejectReasonResponseDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationCancelResponseDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationDetailResponseDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationResponseDto;
import Team_Mute.back_end.domain.reservation.service.ReservationScheduleService;
import Team_Mute.back_end.domain.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;
	private final ReservationScheduleService reservationScheduleService;

	@PostMapping("/available-times")
	public ResponseEntity<AvailableTimeResponse> getAvailableTimes(@Valid @RequestBody AvailableTimeRequest request) {
		List<AvailableTimeResponse.TimeSlot> availableTimes = reservationScheduleService.getAvailableTimes(
			request.getYear(),
			request.getMonth(),
			request.getDay(),
			request.getSpaceId()
		);
		return ResponseEntity.ok(new AvailableTimeResponse(availableTimes));
	}

	@PostMapping("/available-dates")
	public ResponseEntity<AvailableDateResponse> getAvailableDates(@Valid @RequestBody AvailableDateRequest request) {
		List<Integer> availableDays = reservationScheduleService.getAvailableDays(
			request.getYear(),
			request.getMonth(),
			request.getSpaceId()
		);
		return ResponseEntity.ok(new AvailableDateResponse(availableDays));
	}

	// 1. 예약 생성
	@PostMapping(consumes = {"multipart/form-data"})
	public ResponseEntity<ReservationResponseDto> createReservation(
		@AuthenticationPrincipal String userId,
		@Valid @RequestPart("requestDto") ReservationRequestDto requestDto,
		@RequestPart(value = "files", required = false) List<MultipartFile> files) {

		requestDto.setReservationAttachments(files);

		ReservationResponseDto responseDto = reservationService.createReservation(userId, requestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
	}

	// 2. 예약 목록 조회 (전체 or 내 예약)
	@GetMapping
	public ResponseEntity<PagedReservationResponse> getReservations(
		@AuthenticationPrincipal String userId,
		@RequestParam(required = false) String filterOption,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "5") int size) {

		// Spring Data JPA의 Pageable은 페이지 번호를 0부터 시작하므로, 클라이언트가 보내는 1부터 시작하는 페이지 번호를 조정
		Pageable pageable = PageRequest.of(page - 1, size);

		PagedReservationResponse response = reservationService.findReservations(userId, filterOption, pageable);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{reservation_id}")
	public ResponseEntity<ReservationDetailResponseDto> getReservationById(
		@AuthenticationPrincipal String userId,
		@PathVariable("reservation_id") Long reservationId) {

		ReservationDetailResponseDto responseDto = reservationService.findReservationById(userId, reservationId);
		return ResponseEntity.ok(responseDto);
	}

	// 4. 예약 수정
	@PutMapping(value = "/{reservation_id}", consumes = {"multipart/form-data"})
	public ResponseEntity<ReservationResponseDto> updateReservation(
		@AuthenticationPrincipal String userId,
		@PathVariable("reservation_id") Long reservationId,
		@Valid @RequestPart("requestDto") ReservationRequestDto requestDto,
		@RequestPart(value = "files", required = false) List<MultipartFile> files) {

		// DTO에 파일 리스트 설정
		requestDto.setReservationAttachments(files);

		ReservationResponseDto updatedReservation = reservationService.updateReservation(userId, reservationId,
			requestDto);
		return ResponseEntity.ok(updatedReservation);
	}

	// 5. 예약 삭제
	@DeleteMapping("/{reservation_id}") // 경로 변수명 복귀
	public ResponseEntity<Void> deleteReservation(
		@AuthenticationPrincipal String userId,
		@PathVariable("reservation_id") Long reservationId) { // Long 타입으로 받음
		reservationService.deleteReservation(userId, reservationId);
		return ResponseEntity.noContent().build();
	}

	// 6. 예약 취소
	@PostMapping("/cancel/{reservation_id}")
	public ResponseEntity<ReservationCancelResponseDto> cancelReservation(
		@AuthenticationPrincipal String userId,
		@PathVariable("reservation_id") Long reservationId) {

		ReservationCancelResponseDto responseDto = reservationService.cancelReservation(userId, reservationId);
		return ResponseEntity.ok(responseDto);
	}

	@GetMapping("/rejectMassage/{reservation_id}")
	public ResponseEntity<RejectReasonResponseDto> getRejectReason(
		@AuthenticationPrincipal String userId,
		@PathVariable("reservation_id") Long reservationId) {

		RejectReasonResponseDto responseDto = reservationService.findRejectReason(userId, reservationId);
		return ResponseEntity.ok(responseDto);
	}
}
