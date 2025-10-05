package Team_Mute.back_end.domain.space_user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SpaceSearchResponse {
	private final List<SpaceUserResponseDto> meetingRoom; // 미팅룸 (Category ID 1)
	private final List<SpaceUserResponseDto> eventHall;  // 행사장 (Category ID 2)
}
