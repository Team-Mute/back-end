package Team_Mute.back_end.domain.reservation_admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import Team_Mute.back_end.domain.reservation_admin.entity.ReservationLog;

public interface ReservationLogRepository extends JpaRepository<ReservationLog, Long> {
	Optional<ReservationLog> findTopByReservationReservationIdAndChangedStatusReservationStatusIdOrderByRegDateDesc(
		Long reservationId, Long statusId);
}
