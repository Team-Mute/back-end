package mute.backend.domain.spaceadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteSpaceResponse {
  private final String message;
  private final Integer spaceId;
}
