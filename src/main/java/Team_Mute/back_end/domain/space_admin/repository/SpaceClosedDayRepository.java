package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceClosedDay;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * SpaceClosedDay Repository
 * - tb_space_closed_days 테이블 매핑
 * - 공간별 휴무일(Closed Days) 관리
 */
@Repository
public interface SpaceClosedDayRepository extends JpaRepository<SpaceClosedDay, Integer> {
	/**
	 * 특정 공간 ID에 해당하는 모든 휴무일 삭제
	 * - @Modifying 필요 (DELETE는 update 계열이라 명시해야 함)
	 * - clearAutomatically / flushAutomatically 설정: 영속성 컨텍스트 자동 초기화 & flush
	 *
	 * @param spaceId 공간 ID
	 * @return 삭제된 row 개수
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SpaceClosedDay c where c.space.spaceId = :spaceId")
	int deleteBySpaceId(@Param("spaceId") Integer spaceId);

	/**
	 * 특정 공간 ID에 해당하는 휴무일 전체 조회
	 *
	 * @param spaceId 공간 ID
	 * @return 휴무일 리스트
	 */
	@Query("select c from SpaceClosedDay c where c.space.spaceId = :spaceId")
	List<SpaceClosedDay> findAllBySpaceId(@Param("spaceId") Integer spaceId);

	/**
	 * 특정 공간의 특정 월에 해당하는 휴무일 조회
	 * - 조건: 휴무 시작일이 월의 끝보다 빠르고, 휴무 종료일이 월의 시작보다 늦은 경우
	 * → 즉, 해당 월과 겹치는 모든 휴무 기간을 조회
	 *
	 * @param spaceId      공간 ID
	 * @param startOfMonth 조회 시작일 (해당 월의 1일 00:00)
	 * @param endOfMonth   조회 종료일 (해당 월의 마지막 날 23:59:59)
	 * @return 특정 월과 겹치는 휴무일 리스트
	 */
	@Query("SELECT scd FROM SpaceClosedDay scd WHERE scd.space.spaceId = :spaceId AND scd.closedFrom < :endOfMonth AND scd.closedTo > :startOfMonth")
	List<SpaceClosedDay> findBySpaceIdAndMonth(
		@Param("spaceId") Integer spaceId,
		@Param("startOfMonth") LocalDateTime startOfMonth,
		@Param("endOfMonth") LocalDateTime endOfMonth
	);
}
