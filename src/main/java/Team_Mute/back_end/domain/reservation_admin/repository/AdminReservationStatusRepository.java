package Team_Mute.back_end.domain.reservation_admin.repository;

import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminReservationStatusRepository extends JpaRepository<ReservationStatus, Long> {
	Optional<ReservationStatus> findByReservationStatusName(String reservationStatusName);
}
