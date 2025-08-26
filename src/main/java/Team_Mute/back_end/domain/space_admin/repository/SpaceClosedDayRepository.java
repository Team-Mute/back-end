package Team_Mute.back_end.domain.space_admin.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Team_Mute.back_end.domain.space_admin.entity.SpaceClosedDay;

public interface SpaceClosedDayRepository extends JpaRepository<SpaceClosedDay, Integer> {
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SpaceClosedDay c where c.space.spaceId = :spaceId")
	int deleteBySpaceId(@Param("spaceId") Integer spaceId);

	@Query("select c from SpaceClosedDay c where c.space.spaceId = :spaceId")
	List<SpaceClosedDay> findAllBySpaceId(@Param("spaceId") Integer spaceId);

	@Query("SELECT scd FROM SpaceClosedDay scd WHERE scd.space.spaceId = :spaceId AND scd.closedFrom < :endOfMonth AND scd.closedTo > :startOfMonth")
	List<SpaceClosedDay> findBySpaceIdAndMonth(
		@Param("spaceId") Integer spaceId,
		@Param("startOfMonth") LocalDateTime startOfMonth,
		@Param("endOfMonth") LocalDateTime endOfMonth
	);
}
