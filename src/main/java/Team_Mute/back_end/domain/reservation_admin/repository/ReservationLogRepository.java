package Team_Mute.back_end.domain.reservation_admin.repository;

import Team_Mute.back_end.domain.reservation_admin.entity.ReservationLog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationLogRepository extends JpaRepository<ReservationLog, Long> {
}
