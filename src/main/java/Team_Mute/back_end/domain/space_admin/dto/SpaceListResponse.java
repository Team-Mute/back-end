package Team_Mute.back_end.domain.space_admin.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonPropertyOrder({
	"spaceId", "spaceName", "regionName", "categoryName", "userId",
	"spaceCapacity", "spaceLocation", "spaceDescription", "spaceImageUrl", "detailImageUrls",
	"tagNames", "spaceIsAvailable", "reservationWay", "spaceRules", "saveStatus", "operations", "closedDays",
	"regDate", "updDate"
})
public interface SpaceListResponse {
	Integer getSpaceId();

	String getRegionName();

	String getCategoryName();

	Integer getUserId();

	String getSpaceName();

	Integer getSpaceCapacity();

	String getSpaceLocation();

	String getSpaceDescription();

	String getSpaceImageUrl();

	String[] getDetailImageUrls();

	Boolean getSpaceIsAvailable();

	String[] getTagNames();

	String getReservationWay();

	String getSpaceRules();

	String getSaveStatus(); // Enum은 DB에서 문자열

	@JsonRawValue
	String getOperations();

	@JsonRawValue
	String getClosedDays();

	java.time.LocalDateTime getRegDate();

	java.time.LocalDateTime getUpdDate();
}
