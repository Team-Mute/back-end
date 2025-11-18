package Team_Mute.back_end.domain.reservation.repository;

import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 사전답사 예약 리포지토리
 * 사전답사 예약의 CRUD 및 중복 검증, 시간대 조회 기능 제공
 */
@Repository
public interface PrevisitRepository extends JpaRepository<PrevisitReservation, Long> {

	/**
	 * 특정 예약에 사전답사가 존재하는지 확인
	 *
	 * @param reservationId 예약 ID
	 * @return 존재 여부
	 */
	boolean existsByReservationReservationId(Long reservationId);

	/**
	 * 특정 예약의 사전답사 조회
	 *
	 * @param reservationId 예약 ID
	 * @return 사전답사 Optional
	 */
	Optional<PrevisitReservation> findByReservationReservationId(Long reservationId);

	/**
	 * 특정 공간과 시간대에 겹치는 사전답사 존재 여부 확인 (상태 필터링 포함)
	 *
	 * @param spaceId   공간 ID
	 * @param from      시작 시간
	 * @param to        종료 시간
	 * @param statusIds 유효한 예약 상태 ID 리스트 (1,2,3)
	 * @return 중복 여부
	 */
	@Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
		"FROM PrevisitReservation p " +
		"JOIN p.reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND p.previsitTo > :from " +
		"AND p.previsitFrom < :to")
	boolean existsOverlappingPrevisitWithStatus(
		@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("statusIds") List<Long> statusIds
	);

	/**
	 * 비관적 락을 사용하여 겹치는 사전답사 조회
	 * 동시성 제어로 중복 예약 방지
	 *
	 * @return 겹치는 사전답사 리스트
	 */
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

	/**
	 * 특정 예약을 제외하고 겹치는 사전답사 존재 여부 확인
	 * 예약 수정 시 자기 자신을 제외한 중복 검사에 사용
	 *
	 * @param excludeId 제외할 예약 ID
	 * @return 중복 여부
	 */
	@Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
		"FROM PrevisitReservation p " +
		"JOIN p.reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationId != :excludeId " +
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

	/**
	 * 특정 날짜의 유효한 사전답사 목록 조회
	 * 캘린더 UI에서 예약 가능 시간 계산에 사용
	 *
	 * @param spaceId    공간 ID
	 * @param startOfDay 날짜 시작 (00:00)
	 * @param endOfDay   날짜 종료 (23:59)
	 * @param statusIds  유효한 예약 상태 ID 리스트
	 * @return 사전답사 리스트
	 */
	@Query("SELECT pr FROM PrevisitReservation pr JOIN pr.reservation r WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND pr.previsitFrom < :endOfDay AND pr.previsitTo > :startOfDay")
	List<PrevisitReservation> findValidPrevisitsBySpaceAndDay(
		@Param("spaceId") Integer spaceId,
		@Param("startOfDay") LocalDateTime startOfDay,
		@Param("endOfDay") LocalDateTime endOfDay,
		@Param("statusIds") List<Integer> statusIds
	);

	/**
	 * 특정 공간의 모든 사전답사 삭제
	 * 공간 삭제 시 사용 (CASCADE)
	 *
	 * @param spaceId 공간 ID
	 */
	@Modifying(clearAutomatically = true)
	@Transactional
	@Query("DELETE FROM PrevisitReservation pr WHERE pr.reservation.space.id = :spaceId")
	void deletePrevisitReservationsBySpaceId(@Param("spaceId") Integer spaceId);
}
