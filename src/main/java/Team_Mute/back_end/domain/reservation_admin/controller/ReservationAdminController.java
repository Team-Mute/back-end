package Team_Mute.back_end.domain.reservation_admin.controller;

import Team_Mute.back_end.domain.reservation_admin.dto.request.BulkApproveRequestDto;
import Team_Mute.back_end.domain.reservation_admin.dto.request.RejectRequestDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.BulkApproveResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.RejectResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationDetailResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationFilterOptionsResponse;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.service.ReservationAdminService;
import Team_Mute.back_end.global.dto.PagedResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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

/**
 * [관리자 예약 관리] 컨트롤러
 * - 관리자 권한을 기반으로 예약 리스트 조회, 상세 조회, 1/2차 승인 및 반려 처리를 위한 API 엔드포인트를 제공합니다.
 */
@Slf4j
@Tag(name = "예약 관리 API", description = "관리자 예약 관리 관련 API 명세")
@RestController
@RequestMapping("/api/reservations-admin")
public class ReservationAdminController {
	private final ReservationAdminService reservationAdminService;

	/**
	 * 일괄 승인 결과(BulkApproveResponseDto)를 분석하여 HTTP 응답 상태 코드를 결정
	 * (일부 성공/실패 시 207 Multi-Status 반환 등)
	 *
	 * @param resp 일괄 승인 처리 결과 DTO
	 * @return 적절한 HTTP 상태 코드
	 */
	private org.springframework.http.HttpStatus resolveBulkStatus(BulkApproveResponseDto resp) {
		if (resp.getFailureCount() == 0) {
			return org.springframework.http.HttpStatus.OK; // 전부 성공 (200)
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

			if (hasForbidden) return org.springframework.http.HttpStatus.FORBIDDEN;     // 403 권한 문제
			if (hasNotFound) return org.springframework.http.HttpStatus.NOT_FOUND;      // 404 리소스 없음
			if (hasStateConflict) return org.springframework.http.HttpStatus.CONFLICT;  // 409 상태/전이 충돌
			return org.springframework.http.HttpStatus.BAD_REQUEST;                     // 400 그 외 전부 실패
		}

		// 일부 성공 일부 실패 → 부분 성공
		return org.springframework.http.HttpStatus.MULTI_STATUS; // 207
	}

	public ReservationAdminController(ReservationAdminService reservationService) {
		this.reservationAdminService = reservationService;
	}

	/**
	 * 1차 승인 + 2차 승인
	 **/
	@PostMapping("/approve")
	@Operation(summary = "1차 승인 또는 2차 승인(단건 or 일괄)", description = "토큰을 확인하여 1차 승인 및 2차 승인을 진행합니다.")
	public ResponseEntity<BulkApproveResponseDto> approveSecondBulk(
		Authentication authentication,
		@org.springframework.web.bind.annotation.RequestBody BulkApproveRequestDto request
	) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		BulkApproveResponseDto resp = reservationAdminService.approveReservation(adminId, request.getReservationIds());

		return ResponseEntity.status(resolveBulkStatus(resp)).body(resp);
	}

	/**
	 * 예약 반려
	 **/
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

	/**
	 * 예약 상세 조회
	 **/
	@GetMapping("/detail/{reservationId}")
	@Operation(summary = "예약 상세 조회", description = "토큰을 확인하여 예약 상세를 조회합니다.")
	public ResponseEntity<ReservationDetailResponseDto> getById(Authentication authentication, @PathVariable Long reservationId) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());

		return ResponseEntity.ok(reservationAdminService.getByReservationId(adminId, reservationId));
	}

	/**
	 * 1) 필터 옵션 조회: 상태 조회
	 **/
	@GetMapping("/filter-options/statuses")
	@Operation(summary = "예약 상태 필터 옵션 조회", description = "예약 상태 필터링을 위한 드롭다운 옵션을 조회합니다.")
	public ResponseEntity<List<ReservationFilterOptionsResponse.StatusOptionDto>> getStatusOptions() {
		List<ReservationFilterOptionsResponse.StatusOptionDto> statuses = reservationAdminService.getStatusOptions();
		return ResponseEntity.ok(statuses);
	}

	/**
	 * 2) 필터 옵션 조회: 긴급 및 신한 플래그 조회
	 **/
	@GetMapping("/filter-options/flags")
	@Operation(summary = "긴급 및 신한 플래그 조회", description = "긴급/신한 예약 등 플래그 필터링을 위한 드롭다운 옵션을 조회합니다.")
	public ResponseEntity<List<ReservationFilterOptionsResponse.FlagOptionDto>> getFlagOptions() {
		List<ReservationFilterOptionsResponse.FlagOptionDto> flags = reservationAdminService.getFlagOptions();
		return ResponseEntity.ok(flags);
	}

	/**
	 * 복합 조건에 따른 예약 리스트 검색 및 페이징 처리
	 *
	 * @param authentication 현재 로그인된 관리자 정보
	 * @param keyword        키워드 (예약자명, 공간명)
	 * @param regionId       지역 ID (Integer)
	 * @param statusId       상태 ID (Long)
	 * @param isShinhan      신한 예약 여부
	 * @param isEmergency    긴급 예약 여부
	 * @param page           현재 페이지 번호 (1부터 시작)
	 * @param size           페이지 크기 (한 페이지에 보여줄 데이터의 개수)
	 * @return 페이징된 예약 리스트 응답 DTO
	 */
	@GetMapping("/search")
	@Operation(
		summary = "예약 검색(복합 조건)",
		description = "키워드와 함께 지역, 승인 상태, 플래그 등으로 복합 검색합니다."
	)
	public ResponseEntity<PagedResponseDto<ReservationListResponseDto>> searchReservations(
		Authentication authentication,
		@RequestParam(name = "keyword", required = false) String keyword,
		@RequestParam(name = "regionId", required = false) Integer regionId,
		@RequestParam(name = "statusId", required = false) Long statusId,
		@RequestParam(name = "isShinhan", required = false) Boolean isShinhan,
		@RequestParam(name = "isEmergency", required = false) Boolean isEmergency,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "5") int size
	) {
		Pageable pageable = PageRequest.of(
			Math.max(page - 1, 0),
			size,
			Sort.by(Sort.Order.desc("regDate"))
		);
		Long adminId = Long.valueOf((String) authentication.getPrincipal());

		Page<ReservationListResponseDto> data =
			reservationAdminService.searchReservations(adminId, keyword, regionId, statusId, isShinhan, isEmergency, pageable);

		return ResponseEntity.ok(new PagedResponseDto<>(data));
	}
}
