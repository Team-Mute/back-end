package Team_Mute.back_end.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.reservation.entity.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {
	Page<Reservation> findByUser(User user, Pageable pageable);

	@Query("SELECT r FROM Reservation r WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND r.reservationFrom < :endOfMonth AND r.reservationTo > :startOfMonth")
	List<Reservation> findReservationsBySpaceAndMonth(
		@Param("spaceId") Integer spaceId,
		@Param("startOfMonth") LocalDateTime startOfMonth,
		@Param("endOfMonth") LocalDateTime endOfMonth,
		@Param("statusIds") List<Long> statusIds
	);

	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
		"FROM Reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationTo > :from " +
		"AND r.reservationFrom < :to")
	boolean existsOverlappingReservation(@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to);

	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
		"FROM Reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " + // <-- 상태 필터링 조건 추가
		"AND r.reservationTo > :from " +
		"AND r.reservationFrom < :to")
	boolean existsOverlappingReservationWithStatus(
		@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("statusIds") List<Long> statusIds // 상태 ID 리스트를 파라미터로 받음
	);

	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
		"FROM Reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationId != :excludeId " + // <-- 자기 자신은 중복 검사에서 제외
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND r.reservationTo > :from " +
		"AND r.reservationFrom < :to")
	boolean existsOverlappingReservationExcludingSelf(
		@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("statusIds") List<Long> statusIds,
		@Param("excludeId") Long excludeId // 제외할 예약의 ID
	);
}
