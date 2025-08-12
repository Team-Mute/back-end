package mute.backend.domain.spaceadmin.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

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
}
