package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceOperation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceOperationRepository extends JpaRepository<SpaceOperation, Integer> {
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SpaceOperation o where o.space.spaceId = :spaceId")
	int deleteBySpaceId(@Param("spaceId") Integer spaceId);

	@Query("select o from SpaceOperation o where o.space.spaceId = :spaceId")
	List<SpaceOperation> findAllBySpaceId(@Param("spaceId") Integer spaceId);
}
