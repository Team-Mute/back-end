package mute.backend.domain.spaceadmin.repository;

import java.util.Optional;
import mute.backend.domain.spaceadmin.entity.SpaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceCategoryRepository extends JpaRepository<SpaceCategory, Integer> {
  Optional<SpaceCategory> findByCategoryName(String categoryName);
}
