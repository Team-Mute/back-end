package Team_Mute.back_end.domain.reservation_admin.repository;

import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [예약 관리 -> 예약 상태] 리포지토리
 * * 예약 상태(ReservationStatus) 엔티티에 대한 데이터 접근을 담당
 * * 주로 예약 상태 이름(Name)을 기반으로 해당 상태 엔티티를 조회하는 데 사용
 */
public interface AdminReservationStatusRepository extends JpaRepository<ReservationStatus, Long> {
	/**
	 * 주어진 예약 상태 이름과 일치하는 ReservationStatus 엔티티를 조회
	 *
	 * @param reservationStatusName 조회할 예약 상태 이름 (e.g., "1차 승인 대기")
	 * @return 해당 이름과 일치하는 Optional<ReservationStatus>
	 */
	Optional<ReservationStatus> findByReservationStatusName(String reservationStatusName);
}
