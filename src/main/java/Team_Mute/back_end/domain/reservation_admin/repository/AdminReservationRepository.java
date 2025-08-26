package Team_Mute.back_end.domain.reservation_admin.repository;


import Team_Mute.back_end.domain.reservation.entity.Reservation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminReservationRepository extends JpaRepository<Reservation, Long> {
	@Query("select r from Reservation r left join fetch r.previsitReservations where r.reservationId = :id")
	Optional<Reservation> findByIdFetchPrevisits(@Param("id") Long id);
}
