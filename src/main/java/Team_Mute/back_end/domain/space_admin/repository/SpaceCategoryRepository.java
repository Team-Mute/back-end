package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpaceCategoryRepository extends JpaRepository<SpaceCategory, Integer> {
	Optional<SpaceCategory> findByCategoryName(String categoryName);
}
