package Team_Mute.back_end.domain.reservation.controller;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 예약 REST API 컨트롤러
 * 공간 예약 시스템의 사용자 예약 관련 HTTP 엔드포인트 제공
 * <p>
 * 주요 기능:
 * - 예약 가능 날짜 및 시간 조회 (캘린더 UI 지원)
 * - 예약 생성 및 파일 첨부 (multipart/form-data)
 * - 예약 목록 조회 (페이징 및 필터링)
 * - 예약 상세 조회, 취소, 삭제
 * - 반려 사유 조회 (관리자가 반려한 예약)
 * <p>
 * 인증/인가:
 * - 모든 엔드포인트는 JWT 인증 필요
 * - @AuthenticationPrincipal로 사용자 ID 자동 주입
 * - JwtAuthFilter에서 SecurityContext에 설정된 정보 사용
 * <p>
 * API 문서화:
 * - Swagger UI로 자동 문서화 (/swagger-ui.html)
 * - @Tag, @Operation 어노테이션으로 상세 설명 제공
 * <p>
 * 응답 형식:
 * - 성공: 200 OK (조회), 201 Created (생성), 204 No Content (삭제)
 * - 실패: GlobalExceptionHandler에서 처리
 *
 * @author Team Mute
 * @since 1.0
 */
@Tag(name = "예약 API", description = "사용자 예약 관련 API 명세")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

	/**
	 * 예약 비즈니스 로직 서비스
	 * - 예약 생성, 조회, 삭제, 취소 등의 핵심 로직 처리
	 */
	private final ReservationService reservationService;

	/**
	 * 예약 스케줄 서비스
	 * - 예약 가능 날짜 및 시간 계산
	 * - 기존 예약과의 충돌 검증
	 */
	private final ReservationScheduleService reservationScheduleService;

	/**
	 * 예약 가능 시간 조회 API
	 * - 특정 날짜의 예약 가능한 시간대 목록 반환
	 * - 프론트엔드 캘린더에서 시간 선택 시 사용
	 * <p>
	 * 처리 로직:
	 * 1. 년/월/일/공간ID를 받아 해당 날짜의 예약 가능 시간 조회
	 * 2. 기존 예약과 겹치지 않는 시간대 계산
	 * 3. 공간의 운영 시간(openTime, closeTime) 고려
	 * 4. 시간대별로 TimeSlot 리스트 반환
	 *
	 * @param request AvailableTimeRequest DTO (년, 월, 일, 공간ID)
	 * @return ResponseEntity<AvailableTimeResponse> (예약 가능 시간대 목록)
	 */
	@Operation(summary = "예약 가능 시간", description = "특정 일의 예약 가능 시간을 응답합니다.")
	@PostMapping("/available-times")
	public ResponseEntity<AvailableTimeResponse> getAvailableTimes(@Valid @RequestBody AvailableTimeRequest request) {
		// 1. ReservationScheduleService에서 예약 가능 시간 조회
		List<AvailableTimeResponse.TimeSlot> availableTimes = reservationScheduleService.getAvailableTimes(
			request.getYear(),
			request.getMonth(),
			request.getDay(),
			request.getSpaceId()
		);

		// 2. 응답 DTO 생성 및 반환
		return ResponseEntity.ok(new AvailableTimeResponse(availableTimes));
	}

	/**
	 * 예약 가능 날짜 조회 API
	 * - 특정 월의 예약 가능한 날짜(일) 목록 반환
	 * - 프론트엔드 캘린더에서 날짜 표시 시 사용
	 * <p>
	 * 처리 로직:
	 * 1. 년/월/공간ID를 받아 해당 월의 예약 가능 날짜 조회
	 * 2. 공간의 운영 요일(예: 평일만, 주말 포함 등) 고려
	 * 3. 이미 예약이 꽉 찬 날짜는 제외
	 * 4. 과거 날짜 제외
	 * 5. 예약 가능한 날짜(일)의 리스트 반환
	 *
	 * @param request AvailableDateRequest DTO (년, 월, 공간ID)
	 * @return ResponseEntity<AvailableDateResponse> (예약 가능 날짜 목록)
	 */
	@Operation(summary = "예약 가능 날짜", description = "특정 달의 예약 가능 일을 응답합니다.")
	@PostMapping("/available-dates")
	public ResponseEntity<AvailableDateResponse> getAvailableDates(@Valid @RequestBody AvailableDateRequest request) {
		// 1. ReservationScheduleService에서 예약 가능 날짜 조회
		List<Integer> availableDays = reservationScheduleService.getAvailableDays(
			request.getYear(),
			request.getMonth(),
			request.getSpaceId()
		);

		// 2. 응답 DTO 생성 및 반환
		return ResponseEntity.ok(new AvailableDateResponse(availableDays));
	}

	/**
	 * 하루 통째로 예약 가능 날짜 조회 API (예약이 전혀 없는 날)
	 * - 특정 월의 예약이 전혀 없는 날짜(일) 목록 반환
	 * - 캘린더 UI에서 '하루 통째로' 예약 가능한 날을 강조할 때 사용
	 * <p>
	 * 처리 로직:
	 * 1. 년/월/공간ID를 받아 해당 월의 예약 가능한 날짜 조회
	 * 2. 공간의 운영 요일, 휴무일 고려
	 * 3. 기존 예약(일반 + 사전답사)이 하나도 없는 날짜만 필터링
	 * 4. 과거 날짜 제외
	 * 5. 예약 가능한 날짜(일)의 리스트 반환
	 *
	 * @param request AvailableDateRequest DTO (년, 월, 공간ID)
	 * @return ResponseEntity<AvailableDateResponse> (하루 통째로 예약 가능한 날짜 목록)
	 */
	@Operation(summary = "하루 통째로 예약 가능 날짜", description = "특정 달의 예약이 전혀 없는 날짜(일)를 응답합니다.")
	@PostMapping("/fully-available-dates")
	public ResponseEntity<AvailableDateResponse> getFullyAvailableDates(@Valid @RequestBody AvailableDateRequest request) {
		// 1. ReservationScheduleService에서 예약이 전혀 없는 날짜 조회
		List<Integer> fullyAvailableDays = reservationScheduleService.getFullyAvailableDays(
			request.getYear(),
			request.getMonth(),
			request.getSpaceId()
		);

		// 2. 응답 DTO 생성 및 반환
		return ResponseEntity.ok(new AvailableDateResponse(fullyAvailableDays));
	}

	/**
	 * 예약 생성 API
	 * - 새로운 공간 예약 생성 및 첨부 파일 업로드
	 * - multipart/form-data 형식으로 요청 처리
	 * <p>
	 * 처리 로직:
	 * 1. JWT에서 사용자 ID 추출 (@AuthenticationPrincipal)
	 * 2. 예약 정보 DTO와 첨부 파일 리스트 수신
	 * 3. 첨부 파일을 AWS S3에 업로드
	 * 4. 예약 정보를 데이터베이스에 저장 (초기 상태: 승인 대기)
	 * 5. 관리자에게 승인 요청 알림 (선택적)
	 * 6. 생성된 예약 정보 반환
	 * <p>
	 * 예약 초기 상태:
	 * - statusId: 승인 대기 (PENDING_APPROVAL)
	 * - 1차 승인자 및 2차 승인자 승인 필요
	 *
	 * @param userId     인증된 사용자 ID (JWT에서 자동 추출)
	 * @param requestDto 예약 정보 DTO (@Valid로 검증)
	 * @param files      첨부 파일 리스트 (선택적, null 허용)
	 * @return ResponseEntity<ReservationResponseDto> (생성된 예약 정보, 201 Created)
	 */
	@Operation(summary = "예약 생성", description = "예약 정보를 받아 예약을 저장합니다.")
	@PostMapping(consumes = {"multipart/form-data"})
	public ResponseEntity<ReservationResponseDto> createReservation(
		@AuthenticationPrincipal String userId,
		@Valid @RequestPart("requestDto") ReservationRequestDto requestDto,
		@RequestPart(value = "files", required = false) List<MultipartFile> files) {

		// 1. 요청 DTO에 첨부 파일 설정
		requestDto.setReservationAttachments(files);

		// 2. ReservationService에서 예약 생성 처리
		ReservationResponseDto responseDto = reservationService.createReservation(userId, requestDto);

		// 3. 201 Created 상태 코드와 함께 응답 반환
		return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
	}

	/**
	 * 전체 예약 목록 조회 API
	 * - 사용자의 모든 예약 목록을 페이징 및 필터링하여 조회
	 * - 마이페이지에서 예약 내역 확인 시 사용
	 * <p>
	 * 처리 로직:
	 * 1. JWT에서 사용자 ID 추출
	 * 2. 필터 옵션에 따라 예약 목록 필터링
	 * - "pending": 승인 대기
	 * - "approved": 최종 승인
	 * - "rejected": 반려
	 * - "cancelled": 취소
	 * - null: 전체
	 * 3. 페이징 처리 (page, size 파라미터)
	 * 4. 페이징 정보와 함께 예약 목록 반환
	 * <p>
	 * 페이징 처리:
	 * - page와 size 모두 없으면: 페이징 없이 전체 조회
	 * - 하나라도 있으면: 기본값 적용 (page=1, size=5)
	 * - page는 1부터 시작 (내부적으로 0-based로 변환)
	 * <p>
	 * 쿼리 파라미터:
	 * - filterOption: 상태 필터 (선택적)
	 * - page: 페이지 번호 (1부터 시작, 선택적)
	 * - size: 페이지 크기 (선택적)
	 *
	 * @param userId       인증된 사용자 ID (JWT에서 자동 추출)
	 * @param filterOption 상태 필터 (선택적)
	 * @param page         페이지 번호 (선택적, 1부터 시작)
	 * @param size         페이지 크기 (선택적)
	 * @return ResponseEntity<PagedReservationResponse> (페이징된 예약 목록)
	 */
	@Operation(summary = "전체 예약 목록 조회", description = "페이지네이션이 적용된 예약 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<PagedReservationResponse> getReservations(
		@AuthenticationPrincipal String userId,
		@RequestParam(required = false) String filterOption,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size) {

		Pageable pageable;

		// 1. 페이징 파라미터 처리
		// page와 size 파라미터가 모두 제공되지 않은 경우
		if (page == null && size == null) {
			pageable = null; // 페이징 없음을 나타내기 위해 null 전달
		} else {
			// 하나라도 값이 있으면 기본값 적용
			int pageNum = (page != null) ? page : 1;
			int pageSize = (size != null) ? size : 5;

			// PageRequest 생성 (page는 0-based이므로 -1)
			pageable = PageRequest.of(pageNum - 1, pageSize);
		}

		// 2. ReservationService에서 예약 목록 조회
		PagedReservationResponse response = reservationService.findReservations(userId, filterOption, pageable);

		// 3. 응답 반환
		return ResponseEntity.ok(response);
	}

	/**
	 * 개별 예약 상세 조회 API
	 * - 특정 예약의 상세 정보 조회
	 * - 예약 상세 페이지에서 사용
	 * <p>
	 * 처리 로직:
	 * 1. JWT에서 사용자 ID 추출
	 * 2. Path Variable에서 예약 ID 추출
	 * 3. 본인의 예약인지 검증 (다른 사용자의 예약 접근 불가)
	 * 4. 예약 상세 정보 조회 (공간, 첨부파일, 상태 등)
	 * 5. 예약 상세 정보 반환
	 * <p>
	 * 보안:
	 * - 본인의 예약만 조회 가능
	 * - 다른 사용자의 예약 조회 시 403 Forbidden 또는 404 Not Found
	 *
	 * @param userId        인증된 사용자 ID (JWT에서 자동 추출)
	 * @param reservationId 조회할 예약 ID (Path Variable)
	 * @return ResponseEntity<ReservationDetailResponseDto> (예약 상세 정보)
	 */
	@Operation(summary = "개별 예약 조회", description = "예약ID를 받아 하나의 예약 정보를 조회 합니다.")
	@GetMapping("/{reservationId}")
	public ResponseEntity<ReservationDetailResponseDto> getReservationById(
		@AuthenticationPrincipal String userId,
		@PathVariable("reservationId") Long reservationId) {

		// 1. ReservationService에서 예약 상세 정보 조회
		ReservationDetailResponseDto responseDto = reservationService.findReservationById(userId, reservationId);

		// 2. 응답 반환
		return ResponseEntity.ok(responseDto);
	}

	/**
	 * 예약 삭제 API
	 * - 예약 정보를 데이터베이스에서 완전히 삭제 (Hard Delete)
	 * - 승인 대기 상태의 예약만 삭제 가능
	 * <p>
	 * 처리 로직:
	 * 1. JWT에서 사용자 ID 추출
	 * 2. Path Variable에서 예약 ID 추출
	 * 3. 본인의 예약인지 검증
	 * 4. 예약 상태 확인 (승인 대기 상태만 삭제 가능)
	 * 5. 첨부 파일 삭제 (AWS S3)
	 * 6. 예약 정보 삭제 (데이터베이스)
	 * 7. 204 No Content 응답 반환
	 * <p>
	 * 보안:
	 * - 본인의 예약만 삭제 가능
	 * - 다른 사용자의 예약 삭제 시 403 Forbidden
	 *
	 * @param userId        인증된 사용자 ID (JWT에서 자동 추출)
	 * @param reservationId 삭제할 예약 ID (Path Variable)
	 * @return ResponseEntity<Void> (204 No Content)
	 */
	@Operation(summary = "예약 삭제", description = "저장된 예약 정보를 삭제합니다.")
	@DeleteMapping("/{reservationId}")
	public ResponseEntity<Void> deleteReservation(
		@AuthenticationPrincipal String userId,
		@PathVariable("reservationId") Long reservationId) {

		// 1. ReservationService에서 예약 삭제 처리
		reservationService.deleteReservation(userId, reservationId);

		// 2. 204 No Content 응답 반환
		return ResponseEntity.noContent().build();
	}

	/**
	 * 예약 취소 API
	 * - 예약 상태를 취소(CANCELLED) 상태로 변경
	 * - Hard Delete가 아닌 Soft Delete (상태 변경)
	 * <p>
	 * 처리 로직:
	 * 1. JWT에서 사용자 ID 추출
	 * 2. Path Variable에서 예약 ID 추출
	 * 3. 본인의 예약인지 검증
	 * 4. 취소 가능한 상태인지 확인 (승인 대기, 1차 승인, 최종 승인)
	 * 5. 예약 상태를 취소(CANCELLED)로 변경
	 * 6. 관리자에게 취소 알림 (선택적)
	 * 7. 취소 완료 메시지 반환
	 * <p>
	 * 삭제와의 차이:
	 * - 삭제: 데이터베이스에서 완전히 제거 (Hard Delete)
	 * - 취소: 상태만 변경하여 이력 보존 (Soft Delete)
	 *
	 * @param userId        인증된 사용자 ID (JWT에서 자동 추출)
	 * @param reservationId 취소할 예약 ID (Path Variable)
	 * @return ResponseEntity<ReservationCancelResponseDto> (취소 완료 정보)
	 */
	@Operation(summary = "예약 취소", description = "예약 상태를 취소 상태로 수정합니다.")
	@PostMapping("/cancel/{reservationId}")
	public ResponseEntity<ReservationCancelResponseDto> cancelReservation(
		@AuthenticationPrincipal String userId,
		@PathVariable("reservationId") Long reservationId) {

		// 1. ReservationService에서 예약 취소 처리
		ReservationCancelResponseDto responseDto = reservationService.cancelReservation(userId, reservationId);

		// 2. 응답 반환
		return ResponseEntity.ok(responseDto);
	}

	/**
	 * 반려 사유 조회 API
	 * - 관리자가 예약을 반려한 경우 반려 사유 메시지 조회
	 * - 예약 상세 페이지에서 반려 사유 확인 시 사용
	 * <p>
	 * 처리 로직:
	 * 1. JWT에서 사용자 ID 추출
	 * 2. Path Variable에서 예약 ID 추출
	 * 3. 본인의 예약인지 검증
	 * 4. 예약 상태가 반려(REJECTED)인지 확인
	 * 5. 반려 사유 메시지 조회 (rejectReason 필드)
	 * 6. 반려 사유 반환
	 *
	 * @param userId        인증된 사용자 ID (JWT에서 자동 추출)
	 * @param reservationId 조회할 예약 ID (Path Variable)
	 * @return ResponseEntity<RejectReasonResponseDto> (반려 사유 정보)
	 */
	@Operation(summary = "반려 메세지 확인", description = "관리자가 작성한 특정 예약에 대한 반려 메세지를 조회합니다.")
	@GetMapping("/rejectMessage/{reservationId}")
	public ResponseEntity<RejectReasonResponseDto> getRejectReason(
		@AuthenticationPrincipal String userId,
		@PathVariable("reservationId") Long reservationId) {

		// 1. ReservationService에서 반려 사유 조회
		RejectReasonResponseDto responseDto = reservationService.findRejectReason(userId, reservationId);

		// 2. 응답 반환
		return ResponseEntity.ok(responseDto);
	}
}
