package Team_Mute.back_end.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import jakarta.persistence.LockModeType;

@Repository
public interface PrevisitRepository extends JpaRepository<PrevisitReservation, Long> {

	boolean existsByReservationReservationId(Long reservationId);

	Optional<PrevisitReservation> findByReservationReservationId(Long reservationId);

	@Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
		"FROM PrevisitReservation p " +
		"JOIN p.reservation r " + // PrevisitReservation과 Reservation을 명시적으로 조인
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " + // <-- 연결된 본 예약의 상태 필터링
		"AND p.previsitTo > :from " +
		"AND p.previsitFrom < :to")
	boolean existsOverlappingPrevisitWithStatus(
		@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("statusIds") List<Long> statusIds
	);

	// 비관적 락을 사용한 중복 사전답사 조회 쿼리 추가
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM PrevisitReservation p " +
		"JOIN p.reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND p.previsitTo > :from " +
		"AND p.previsitFrom < :to")
	List<PrevisitReservation> findOverlappingPrevisitsWithLock(
		@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("statusIds") List<Long> statusIds
	);

	@Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
		"FROM PrevisitReservation p " +
		"JOIN p.reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationId != :excludeId " + // <-- 자기 자신과 연결된 사전답사는 제외
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND p.previsitTo > :from " +
		"AND p.previsitFrom < :to")
	boolean existsOverlappingPrevisitExcludingSelf(
		@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("statusIds") List<Long> statusIds,
		@Param("excludeId") Long excludeId
	);

	@Query("SELECT pr FROM PrevisitReservation pr JOIN pr.reservation r WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " + // 본 예약 상태 검증 조건 추가
		"AND pr.previsitFrom < :endOfDay AND pr.previsitTo > :startOfDay")
	List<PrevisitReservation> findValidPrevisitsBySpaceAndDay(
		@Param("spaceId") Integer spaceId,
		@Param("startOfDay") LocalDateTime startOfDay,
		@Param("endOfDay") LocalDateTime endOfDay,
		@Param("statusIds") List<Long> statusIds // 유효한 상태 ID 목록을 파라미터로 받음
	);
}
