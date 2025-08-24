package Team_Mute.back_end.domain.space_admin.dto;


import Team_Mute.back_end.domain.member.entity.AdminRegion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminRegionDto {
	private Integer regionId;
	private String regionName;

	public static AdminRegionDto fromEntity(AdminRegion adminRegion) {
		return AdminRegionDto.builder()
			.regionId(adminRegion.getRegionId())
			.regionName(adminRegion.getRegionName())
			.build();
	}
}
