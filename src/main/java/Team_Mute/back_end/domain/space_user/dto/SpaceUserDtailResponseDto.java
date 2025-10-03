package Team_Mute.back_end.domain.space_user.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonPropertyOrder({
	"spaceId", "spaceName", "regionName", "categoryName", "spaceCapacity", "spaceDescription",
	"spaceImageUrl", "reservationWay", "spaceRules", "manager",
	"location", "detailImageUrls", "tagNames", "operations", "closedDays",
})
public interface SpaceUserDtailResponseDto {
	Integer getSpaceId();

	String getSpaceName();

	String getRegionName();

	String getCategoryName();

	Integer getSpaceCapacity();

	String getSpaceDescription();

	String getSpaceImageUrl();

	String getReservationWay();

	String getSpaceRules();

	@JsonRawValue
	String getManager(); // JSON text: {"managerName":"홍길동", "managerPhone":"010-1234-5678"}

	@JsonRawValue
	String getLocation(); // JSON text: {"locationName":"...", "addressRoad":"..."}

	String[] getDetailImageUrls();

	String[] getTagNames();

	@JsonRawValue
	String getOperations();

	@JsonRawValue
	String getClosedDays();
}
