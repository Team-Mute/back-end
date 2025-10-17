package Team_Mute.back_end.domain.reservation_admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [예약 관리 -> 사전 답사] 리포지토리
 * * 사전 답사(PrevisitReservation) 엔티티에 대한 데이터 접근을 담당
 * * 주로 예약 관리 리스트에서 예약 ID 목록(reservationIds)에 해당하는 사전 방문 기록들을 조회하는 데 사용
 */
public interface AdminPrevisitReservationRepository extends JpaRepository<PrevisitReservation, Long> {
	/**
	 * 주어진 예약 ID 목록(Iterable<Long>)에 연결된 모든 사전 답사 기록을 조회
	 *
	 * @param reservationIds 조회할 예약 ID 목록
	 * @return 해당 예약들에 연결된 PrevisitReservation 리스트
	 */
	List<PrevisitReservation> findByReservation_ReservationIdIn(Iterable<Long> reservationIds);
}
