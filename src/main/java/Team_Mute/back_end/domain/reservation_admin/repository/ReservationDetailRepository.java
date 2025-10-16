package Team_Mute.back_end.domain.reservation_admin.repository;

import Team_Mute.back_end.domain.reservation.entity.Reservation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * [예약 관리 -> 상세 조회] 전용 리포지토리
 * * 예약 상세 화면을 구성하기 위해 Reservation 엔티티 조회 시
 * 사용자(user), 공간(space), 예약 상태(reservationStatus) 엔티티를
 * 모두 Fetch Join하여 즉시 로딩하는 기능을 제공
 */
public interface ReservationDetailRepository extends JpaRepository<Reservation, Long> {

	/**
	 * 예약 상세 정보를 위해 Reservation 엔티티를 조회하면서,
	 * 사용자(user), 공간(space), 예약 상태(reservationStatus)를 Fetch Join하여 즉시 로딩
	 *
	 * @param id 조회할 예약 ID (reservationId)
	 * @return 연관 엔티티가 Fetch Join된 Optional<Reservation>
	 */
	@Query("""
		select r from Reservation r
		left join fetch r.user u
		left join fetch r.space s
		left join fetch r.reservationStatus rs
		where r.id = :id or r.reservationId = :id
		""")
	Optional<Reservation> findDetailById(@Param("id") Long id);
}
