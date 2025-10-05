package Team_Mute.back_end.domain.space_user.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonPropertyOrder({
	"spaceId", "spaceName", "spaceDescription",
	"spaceCapacity", "categoryId", "categoryName", "tagNames", "location", "spaceImageUrl"
})
public interface SpaceUserResponseDto {

	Integer getSpaceId();

	String getSpaceName();

	String getSpaceDescription();

	Integer getSpaceCapacity();

	Integer getCategoryId();

	String getCategoryName();

	String[] getTagNames();

	@JsonRawValue
	String getLocation(); // JSON text: {"locationName":"...", "addressRoad":"..."}

	String getSpaceImageUrl();
}
