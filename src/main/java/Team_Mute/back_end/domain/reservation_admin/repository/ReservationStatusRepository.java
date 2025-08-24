package Team_Mute.back_end.domain.reservation_admin.repository;

import Team_Mute.back_end.domain.reservation_admin.entity.ReservationStatus;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationStatusRepository extends JpaRepository<ReservationStatus, Integer> {
	Optional<ReservationStatus> findByReservationStatusName(String reservationStatusName);
}
