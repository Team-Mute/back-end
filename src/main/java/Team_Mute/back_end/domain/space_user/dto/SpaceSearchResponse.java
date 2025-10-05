package Team_Mute.back_end.domain.space_user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 공간 검색 결과 응답 DTO
 * - 카테고리별(미팅룸, 행사장)로 검색 결과를 분리하여 제공
 * - Lombok의 {@code @Getter}, {@code @Builder}를 사용하여 boilerplate 코드를 단축
 */
@Getter
@Builder
public class SpaceSearchResponse {
	/**
	 * 미팅룸 공간 리스트(category_id = 1)
	 */
	private final List<SpaceUserResponseDto> meetingRoom;

	/**
	 * 행사장 공간 리스트(category_id = 2)
	 */
	private final List<SpaceUserResponseDto> eventHall;
}
