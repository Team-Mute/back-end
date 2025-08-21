package Team_Mute.back_end.domain.space_user.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
	"spaceId", "spaceName", "spaceDescription",
	"spaceCapacity", "categoryName", "tagNames", "spaceImageUrl"
})
public interface SpaceUserResponseDto {

	Integer getSpaceId();

	String getSpaceName();

	String getSpaceDescription();

	Integer getSpaceCapacity();

	String getCategoryName();

	String[] getTagNames();

	String getSpaceImageUrl();
}
