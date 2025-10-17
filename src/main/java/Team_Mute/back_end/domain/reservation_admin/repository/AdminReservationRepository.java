package Team_Mute.back_end.domain.reservation_admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * [예약 관리] 기본 리포지토리
 * - 예약(Reservation) 엔티티의 기본적인 CRUD 및 사전 답사(PrevisitReservations)를 즉시 로딩(Fetch Join)하는 특수 조회 기능을 제공
 */
public interface AdminReservationRepository extends JpaRepository<Reservation, Long> {
	/**
	 * 특정 예약 ID에 해당하는 Reservation 엔티티를 조회하면서,
	 * 연관된 사전 답사 기록(previsitReservations)을 Fetch Join하여 즉시 로딩
	 * (N+1 문제 방지 목적)
	 *
	 * @param id 조회할 예약 ID
	 * @return 사전 방문 기록이 Fetch Join된 Optional<Reservation>
	 */
	@Query("select r from Reservation r left join fetch r.previsitReservations where r.reservationId = :id")
	Optional<Reservation> findByIdFetchPrevisits(@Param("id") Long id);
}
