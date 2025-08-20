package Team_Mute.back_end.domain.space_admin.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
	"spaceId", "spaceName", "regionName", "userName",
	"spaceImageUrl", "spaceIsAvailable",
})
public interface SpaceListResponse {
	Integer getSpaceId();

	String getSpaceName();

	String getRegionName();

	String getUserName();

	String getSpaceImageUrl();

	Boolean getSpaceIsAvailable();
}
