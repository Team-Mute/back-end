package Team_Mute.back_end.domain.previsit.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;

@Repository
public interface PrevisitRepository extends JpaRepository<PrevisitReservation, Long> {

	boolean existsByReservationReservationId(Long reservationId);

	Optional<PrevisitReservation> findByReservationReservationId(Long reservationId);

	@Query("SELECT pr FROM PrevisitReservation pr JOIN pr.reservation r WHERE r.space.spaceId = :spaceId " +
		"AND pr.previsitFrom < :endOfDay AND pr.previsitTo > :startOfDay")
	List<PrevisitReservation> findPrevisitsBySpaceAndDay(
		@Param("spaceId") Integer spaceId,
		@Param("startOfDay") LocalDateTime startOfDay,
		@Param("endOfDay") LocalDateTime endOfDay
	);

	@Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
		"FROM PrevisitReservation p " +
		"WHERE p.reservation.space.spaceId = :spaceId " +
		"AND p.previsitTo > :from " +
		"AND p.previsitFrom < :to")
	boolean existsOverlappingPrevisit(@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to);
}
