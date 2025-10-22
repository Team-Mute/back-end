package Team_Mute.back_end.domain.reservation.repository;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

	// 비관적 락을 사용한 중복 예약 조회 쿼리 추가
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND r.reservationTo > :from " +
		"AND r.reservationFrom < :to")
	List<Reservation> findOverlappingReservationsWithLock(
		@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("statusIds") List<Long> statusIds
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

	Optional<Reservation> findByReservationId(Long reservationId);


	/**
	 * [공간 삭제] 시 사용되는 메소드 그룹
	 * Space를 삭제하기 전, 관련된 예약 및 연관 데이터를 처리합니다.
	 *
	 **/
	// 1. 활성 예약 (1, 2, 3) 존재 여부 체크
	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM Reservation r WHERE r.space.id = :spaceId AND r.reservationStatus.id IN (1, 2, 3)")
	boolean findCountOfActiveReservations(@Param("spaceId") Integer spaceId);

	// 2. 과거 예약 (4, 5, 6) 존재 여부 체크
	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM Reservation r WHERE r.space.id = :spaceId AND r.reservationStatus.id IN (4, 5, 6)")
	boolean findCountOfPastReservations(@Param("spaceId") Integer spaceId);

	// 3. Space를 참조하는 예약 데이터 모두 삭제 (Deletion)
	@Modifying(clearAutomatically = true)
	@Transactional
	@Query("DELETE FROM Reservation r WHERE r.space.id = :spaceId")
	void deleteReservationsBySpaceId(@Param("spaceId") Integer spaceId);
}
