package Team_Mute.back_end.domain.invitation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitationResponseDto {
	private final String userName;
	private final String spaceName;
	private final String addressRoad;
	private final LocalDateTime reservationFrom;
	private final LocalDateTime reservationTo;
	private final String reservationPurpose;
	private final List<String> reservationAttachment;
}
