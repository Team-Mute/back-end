package Team_Mute.back_end.domain.reservation_admin.repository;


import Team_Mute.back_end.domain.reservation.entity.Reservation;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [예약 관리] 기본 리포지토리
 * - 예약(Reservation) 엔티티의 기본적인 CRUD 및 사전 답사(PrevisitReservations)를 즉시 로딩(Fetch Join)하는 특수 조회 기능을 제공
 */
public interface AdminReservationRepository extends JpaRepository<Reservation, Long> {

}
