package Team_Mute.back_end.domain.space_admin.dto;

public record LocationListItem(
	Integer locationId,
	String locationName,
	String addressRoad,
	String postalCode) {
}
