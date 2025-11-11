package Team_Mute.back_end.domain.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;

/**
 * 예약 상태 리포지토리
 * 예약 상태 코드 테이블 접근
 *
 * 기본 CRUD만 사용하며 커스텀 메서드 없음
 */
@Repository
public interface ReservationStatusRepository extends JpaRepository<ReservationStatus, Long> {
}
