package Team_Mute.back_end.domain.space_admin.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
	"spaceId", "spaceName", "regionName", "regionId", "adminName",
	"spaceImageUrl", "spaceIsAvailable"
})
public interface SpaceListResponseDto {
	Integer getSpaceId();

	String getSpaceName();

	String getRegionName();

	Integer getRegionId();

	String getAdminName();

	String getSpaceImageUrl();

	Boolean getSpaceIsAvailable();
}
