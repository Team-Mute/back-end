package Team_Mute.back_end.domain.reservation_admin.repository;


import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminPrevisitReservationRepository extends JpaRepository<PrevisitReservation, Long> {
	List<PrevisitReservation> findByReservation_ReservationIdIn(Iterable<Long> reservationIds);
}
