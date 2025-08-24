package Team_Mute.back_end.domain.reservation_admin.repository;

import Team_Mute.back_end.domain.reservation_admin.entity.Reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
}
