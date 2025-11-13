package Team_Mute.back_end.domain.invitation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Team_Mute.back_end.domain.invitation.dto.response.InvitationResponseDto;
import Team_Mute.back_end.domain.invitation.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 초대장 관련 요청을 처리하는 컨트롤러입니다.
 * 완료된 예약에 대한 초대장 조회 기능을 제공
 */
@Tag(name = "초대장 API", description = "완료된 예약에 대한 초대장 관련 API 명세")
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

	private final InvitationService invitationService;

	/**
	 * 초대장 조회
	 * - 예약 완료된 특정 예약에 대한 초대장 정보를 조회
	 * - 예약 ID를 통해 해당 예약의 초대장 상세 정보를 반환
	 *
	 * @param reservationId 조회할 예약의 고유 식별자 (예: 123)
	 * @return 초대장 상세 정보 DTO를 포함하는 {@code ResponseEntity}
	 */
	@Operation(summary = "초대장 조회", description = "예약 완료된 예약에 대한 초대장을 조회합니다.")
	@GetMapping("/{reservationId}")
	public ResponseEntity<InvitationResponseDto> getInvitation(
		@PathVariable("reservationId") Long reservationId
	) {
		InvitationResponseDto invitationDetails = invitationService.getInvitationDetails(reservationId);
		return ResponseEntity.ok(invitationDetails);
	}
}
