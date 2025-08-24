package Team_Mute.back_end.domain.reservation_admin.repository;

import Team_Mute.back_end.domain.reservation_admin.entity.PrevisitReservation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrevisitReservationRepository extends JpaRepository<PrevisitReservation, Integer> {
	List<PrevisitReservation> findByReservation_ReservationIdIn(Iterable<Integer> reservationIds);
}
