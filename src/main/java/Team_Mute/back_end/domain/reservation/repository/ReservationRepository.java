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

/**
 * 예약 리포지토리
 * 예약의 CRUD, 중복 검증, 필터링 조회 기능 제공
 * ReservationRepositoryCustom을 확장하여 QueryDSL 기반 동적 쿼리 지원
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {

	/**
	 * 특정 사용자의 예약 목록 조회 (페이징)
	 *
	 * @param user     사용자 엔티티
	 * @param pageable 페이징 정보
	 * @return 예약 페이지
	 */
	Page<Reservation> findByUser(User user, Pageable pageable);

	/**
	 * 특정 공간의 특정 월 예약 목록 조회 (상태 필터링 포함)
	 * 캘린더 UI에서 월별 예약 현황 표시에 사용
	 *
	 * @param spaceId      공간 ID
	 * @param startOfMonth 월 시작일 (1일 00:00)
	 * @param endOfMonth   월 종료일 (말일 23:59)
	 * @param statusIds    유효한 예약 상태 ID 리스트
	 * @return 예약 리스트
	 */
	@Query("SELECT r FROM Reservation r WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND r.reservationFrom < :endOfMonth AND r.reservationTo > :startOfMonth")
	List<Reservation> findReservationsBySpaceAndMonth(
		@Param("spaceId") Integer spaceId,
		@Param("startOfMonth") LocalDateTime startOfMonth,
		@Param("endOfMonth") LocalDateTime endOfMonth,
		@Param("statusIds") List<Integer> statusIds
	);

	/**
	 * 특정 공간과 시간대에 겹치는 예약 존재 여부 확인
	 *
	 * @param spaceId 공간 ID
	 * @param from    시작 시간
	 * @param to      종료 시간
	 * @return 중복 여부
	 */
	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
		"FROM Reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationTo > :from " +
		"AND r.reservationFrom < :to")
	boolean existsOverlappingReservation(@Param("spaceId") Integer spaceId,
										 @Param("from") LocalDateTime from,
										 @Param("to") LocalDateTime to);

	/**
	 * 특정 공간과 시간대에 겹치는 예약 존재 여부 확인 (상태 필터링 포함)
	 *
	 * @param statusIds 유효한 예약 상태 ID 리스트 (1,2,3)
	 * @return 중복 여부
	 */
	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
		"FROM Reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND r.reservationTo > :from " +
		"AND r.reservationFrom < :to")
	boolean existsOverlappingReservationWithStatus(
		@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("statusIds") List<Long> statusIds
	);

	/**
	 * 비관적 락을 사용하여 겹치는 예약 조회
	 * 동시성 제어로 중복 예약 방지 (PESSIMISTIC_WRITE)
	 *
	 * @return 겹치는 예약 리스트
	 */
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

	/**
	 * 특정 예약을 제외하고 겹치는 예약 존재 여부 확인
	 * 예약 수정 시 자기 자신을 제외한 중복 검사에 사용
	 *
	 * @param excludeId 제외할 예약 ID
	 * @return 중복 여부
	 */
	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
		"FROM Reservation r " +
		"WHERE r.space.spaceId = :spaceId " +
		"AND r.reservationId != :excludeId " +
		"AND r.reservationStatus.reservationStatusId IN :statusIds " +
		"AND r.reservationTo > :from " +
		"AND r.reservationFrom < :to")
	boolean existsOverlappingReservationExcludingSelf(
		@Param("spaceId") Integer spaceId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("statusIds") List<Long> statusIds,
		@Param("excludeId") Long excludeId
	);

	/**
	 * 예약 ID로 예약 조회
	 *
	 * @param reservationId 예약 ID
	 * @return 예약 Optional
	 */
	Optional<Reservation> findByReservationId(Long reservationId);

	/**
	 * 특정 공간의 활성 예약 (승인 대기, 1차 승인, 최종 승인) 존재 여부 확인
	 *
	 * @param spaceId 공간 ID
	 * @return 활성 예약 존재 여부
	 */
	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM Reservation r WHERE r.space.id = :spaceId AND r.reservationStatus.id IN (1, 2, 3)")
	boolean findCountOfActiveReservations(@Param("spaceId") Integer spaceId);

	/**
	 * 특정 공간의 과거 예약 (반려, 이용완료, 취소) 존재 여부 확인
	 *
	 * @param spaceId 공간 ID
	 * @return 과거 예약 존재 여부
	 */
	@Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM Reservation r WHERE r.space.id = :spaceId AND r.reservationStatus.id IN (4, 5, 6)")
	boolean findCountOfPastReservations(@Param("spaceId") Integer spaceId);

	/**
	 * 특정 공간의 모든 예약 삭제
	 * 공간 삭제 시 사용 (CASCADE)
	 *
	 * @param spaceId 공간 ID
	 */
	@Modifying(clearAutomatically = true)
	@Transactional
	@Query("DELETE FROM Reservation r WHERE r.space.id = :spaceId")
	void deleteReservationsBySpaceId(@Param("spaceId") Integer spaceId);
}
