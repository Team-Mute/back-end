package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceCategory;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceCategoryRepository extends JpaRepository<SpaceCategory, Integer> {
	Optional<SpaceCategory> findByCategoryId(Integer categoryId);
}
