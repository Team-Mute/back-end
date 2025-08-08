package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTagMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SpaceTagMapRepository extends JpaRepository<SpaceTagMap, Integer> {

	@Transactional
	void deleteBySpace(Space space);
}
