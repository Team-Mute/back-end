package Team_Mute.back_end.domain.space_admin.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
	"spaceId", "spaceName", "regionName", "adminName",
	"spaceImageUrl", "spaceIsAvailable",
})
public interface SpaceListResponse {
	Integer getSpaceId();

	String getSpaceName();

	String getRegionName();

	String getAdminName();

	String getSpaceImageUrl();

	Boolean getSpaceIsAvailable();
}
