package Team_Mute.back_end.domain.dashboard_admin.repository;

import Team_Mute.back_end.domain.reservation.entity.Reservation;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DashboardAdminRepository extends JpaRepository<Reservation, Long> {

	/**
	 * [캘린더 리스트 조회를 위한 기간 및 상태 필터링 쿼리]
	 * * 예약 시작일 또는 종료일이 주어진 기간(start ~ end)에 걸치는 모든 예약을 조회합니다.
	 * 특정 예약 상태 ID 목록(statusIds)에 포함되는 예약만 조회합니다.
	 *
	 * @param start 조회 시작 시간 (해당 월의 1일 00:00:00)
	 * @param end   조회 종료 시간 (해당 월의 마지막 날 23:59:59)
	 * @return 기간 및 상태가 필터링된 Reservation 엔티티 리스트
	 */
	@Query("SELECT r FROM Reservation r " +
		"JOIN FETCH r.reservationStatus rs WHERE " + // 상태 엔티티를 조인하여 N+1 문제 방지
		// 예약 기간이 조회 기간에 걸치는 조건
		"(r.reservationFrom BETWEEN :start AND :end OR r.reservationTo BETWEEN :start AND :end)"
	)
	List<Reservation> findReservationsByPeriodAndStatus(
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end
	);
}
