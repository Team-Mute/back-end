package Team_Mute.back_end.domain.space_admin.dto;

import Team_Mute.back_end.domain.space_admin.entity.SaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SpaceCreateRequest {
	private String spaceName;
	private String spaceLocation;
	private String spaceDescription;
	private Integer spaceCapacity;
	private Boolean spaceIsAvailable;
	private String regionName;
	private Integer regionId;
	private Integer categoryId;
	private String categoryName;
	private List<String> tagNames;
	private Integer userId;
	private String imageUrl;

	@Size(max = 5000) // 길이 여유
	private String reservationWay;

	@Size(max = 5000) // 길이 여유
	private String spaceRules;

	@NotNull(message = "saveStatus는 필수입니다")
	private SaveStatus saveStatus;
}

