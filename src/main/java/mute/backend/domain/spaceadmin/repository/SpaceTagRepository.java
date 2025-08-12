package mute.backend.domain.spaceadmin.repository;

import java.util.Optional;
import mute.backend.domain.spaceadmin.entity.SpaceTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceTagRepository extends JpaRepository<SpaceTag, Integer> {
  Optional<SpaceTag> findByTagName(String tagName);
}
