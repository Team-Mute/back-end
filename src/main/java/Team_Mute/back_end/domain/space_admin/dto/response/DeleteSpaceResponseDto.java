package Team_Mute.back_end.domain.space_admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteSpaceResponseDto {
	private final String message;
	private final Integer spaceId;
}

