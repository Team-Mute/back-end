package Team_Mute.back_end.domain.space_admin.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
	"spaceId", "spaceName", "regionName", "categoryName", "userId",
	"spaceCapacity", "spaceLocation", "spaceDescription", "spaceImageUrl", "detailImageUrls",
	"tagNames", "spaceIsAvailable", "regDate", "updDate"
})
public interface SpaceListResponse {
	Integer getSpaceId();
	String  getRegionName();
	String  getCategoryName();
	Integer getUserId();
	String  getSpaceName();
	Integer getSpaceCapacity();
	String  getSpaceLocation();
	String  getSpaceDescription();
	String  getSpaceImageUrl();
	String[] getDetailImageUrls();
	Boolean getSpaceIsAvailable();
	String[] getTagNames();
	java.time.LocalDateTime getRegDate();
	java.time.LocalDateTime getUpdDate();
}
