package Team_Mute.back_end.domain.reservation_admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;

public interface AdminReservationStatusRepository extends JpaRepository<ReservationStatus, Integer> {
	Optional<ReservationStatus> findByReservationStatusName(String reservationStatusName);
}
