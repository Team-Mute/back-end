package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceClosedDay;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceClosedDayRepository extends JpaRepository<SpaceClosedDay, Integer> {
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SpaceClosedDay c where c.space.spaceId = :spaceId")
	int deleteBySpaceId(@Param("spaceId") Integer spaceId);

	@Query("select c from SpaceClosedDay c where c.space.spaceId = :spaceId")
	List<SpaceClosedDay> findAllBySpaceId(@Param("spaceId") Integer spaceId);
}
