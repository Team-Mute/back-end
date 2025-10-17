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

@Tag(name = "초대장 API", description = "완료된 예약에 대한 초대장관련 API 입니다.")
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

	private final InvitationService invitationService;

	@Operation(summary = "초대장 조회", description = "예약 완료된 예약에 대한 초대장을 조회합니다.")
	@GetMapping("/{reservation_id}")
	public ResponseEntity<InvitationResponseDto> getInvitation(@PathVariable("reservation_id") Long reservationId) {
		InvitationResponseDto invitationDetails = invitationService.getInvitationDetails(reservationId);
		return ResponseEntity.ok(invitationDetails);
	}
}
