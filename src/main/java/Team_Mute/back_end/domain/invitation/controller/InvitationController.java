package Team_Mute.back_end.domain.invitation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Team_Mute.back_end.domain.invitation.dto.response.InvitationResponseDto;
import Team_Mute.back_end.domain.invitation.service.InvitationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

	private final InvitationService invitationService;

	@GetMapping("/{reservation_id}")
	public ResponseEntity<InvitationResponseDto> getInvitation(@PathVariable("reservation_id") Long reservationId) {
		InvitationResponseDto invitationDetails = invitationService.getInvitationDetails(reservationId);
		return ResponseEntity.ok(invitationDetails);
	}
}
