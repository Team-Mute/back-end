package Team_Mute.back_end.domain.space_admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteSpaceResponse {
	private final String message;
	private final Integer spaceId;
}

