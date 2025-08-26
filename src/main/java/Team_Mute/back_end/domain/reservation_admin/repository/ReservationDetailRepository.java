package Team_Mute.back_end.domain.reservation_admin.repository;

import Team_Mute.back_end.domain.reservation.entity.Reservation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationDetailRepository extends JpaRepository<Reservation, Long> {


	@Query("""
		select r from Reservation r
		left join fetch r.user u
		left join fetch r.space s
		left join fetch r.reservationStatus rs
		where r.id = :id or r.reservationId = :id
		""")
	Optional<Reservation> findDetailById(@Param("id") Long id);
}
