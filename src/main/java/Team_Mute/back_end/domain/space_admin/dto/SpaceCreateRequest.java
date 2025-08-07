package Team_Mute.back_end.domain.space_admin.dto;

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
	private Boolean spaceAvailable;
	private String regionName;
	private String categoryName;
	private List<String> tagNames;
	private Integer userId;
}

