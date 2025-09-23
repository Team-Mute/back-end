package Team_Mute.back_end.domain.space_admin.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonPropertyOrder({
	"spaceId", "spaceName", "region", "category", "location", "adminName",
	"spaceCapacity", "spaceLocation", "spaceDescription", "spaceImageUrl", "detailImageUrls",
	"tagNames", "spaceIsAvailable", "reservationWay", "spaceRules", "operations", "closedDays",
	"regDate", "updDate"
})
public interface SpaceDatailResponseDto {
	Integer getSpaceId();

	String getAdminName();

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

	@JsonRawValue
	String getRegion();   // JSON text: {"regionId":1,"regionName":"명동"}

	@JsonRawValue
	String getCategory(); // JSON text: {"categoryId":1,"categoryName":"미팅룸"}

	@JsonRawValue
	String getLocation(); // JSON text: {"locationId":1,"addressRoad":"..."}
}
