package Team_Mute.back_end.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.reservation.entity.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	Page<Reservation> findByUser(User user, Pageable pageable);

	@Query("SELECT r FROM Reservation r WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND r.reservationFrom < :endOfMonth AND r.reservationTo > :startOfMonth")
	List<Reservation> findReservationsBySpaceAndMonth(
		@Param("spaceId") Integer spaceId,
		@Param("startOfMonth") LocalDateTime startOfMonth,
		@Param("endOfMonth") LocalDateTime endOfMonth,
		@Param("statusIds") List<Long> statusIds
	);
}
