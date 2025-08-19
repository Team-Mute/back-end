package Team_Mute.back_end.domain.space_admin.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonPropertyOrder({
	"spaceId", "spaceName", "regionName", "categoryName", "addressRoad", "userId",
	"spaceCapacity", "spaceDescription", "spaceImageUrl", "detailImageUrls",
	"tagNames", "spaceIsAvailable", "reservationWay", "spaceRules", "operations", "closedDays",
	"regDate", "updDate"
})
public interface SpaceListResponse {
	Integer getSpaceId();

	String getRegionName();

	String getCategoryName();

	String getAddressRoad();

	Integer getUserId();

	String getSpaceName();

	Integer getSpaceCapacity();

	String getSpaceDescription();

	String getSpaceImageUrl();

	String[] getDetailImageUrls();

	Boolean getSpaceIsAvailable();

	String[] getTagNames();

	String getReservationWay();

	String getSpaceRules();

	@JsonRawValue
	String getOperations();

	@JsonRawValue
	String getClosedDays();

	java.time.LocalDateTime getRegDate();

	java.time.LocalDateTime getUpdDate();
}
