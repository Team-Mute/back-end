package Team_Mute.back_end.domain.space_admin.dto.response;

public record LocationListResponseDto(
	Integer locationId,
	String locationName,
	String addressRoad,
	String postalCode) {
}
