package Team_Mute.back_end.domain.reservation_admin.repository;

import Team_Mute.back_end.domain.reservation_admin.entity.ReservationLog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [예약 관리 -> 반려 사유 로그] 리포지토리
 * * 예약 상태 변경 등의 기록을 담는 ReservationLog 엔티티에 대한 데이터 접근을 담당
 * * 주로 특정 상태로 변경된 가장 최근 로그를 조회하는 데 사용
 */
public interface ReservationLogRepository extends JpaRepository<ReservationLog, Long> {
	/**
	 * 특정 예약({@code reservationId})에 대해, 특정 상태({@code statusId})로 변경된 기록 중
	 * 가장 최근의(최신 {@code regDate}) 로그를 조회
	 *
	 * @param reservationId 조회할 예약의 ID
	 * @param statusId      조회할 변경 상태의 ID
	 * @return 가장 최근의 ReservationLog 기록 Optional
	 */
	Optional<ReservationLog> findTopByReservationReservationIdAndChangedStatusReservationStatusIdOrderByRegDateDesc(
		Long reservationId, Long statusId);
}
