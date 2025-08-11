package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpaceTagRepository extends JpaRepository<SpaceTag, Integer> {
	Optional<SpaceTag> findByTagName(String tagName);
}
