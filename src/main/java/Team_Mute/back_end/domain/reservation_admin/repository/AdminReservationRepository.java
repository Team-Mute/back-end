package Team_Mute.back_end.domain.reservation_admin.repository;


import Team_Mute.back_end.domain.reservation.entity.Reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminReservationRepository extends JpaRepository<Reservation, Long> {
}
