package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceOperation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * SpaceOperation Repository
 * - tb_space_operation 테이블 매핑
 * - 공간 운영 시간(요일별 open/close) 관리
 */
@Repository
public interface SpaceOperationRepository extends JpaRepository<SpaceOperation, Integer> {

	/**
	 * 특정 공간(SpaceId) 기준으로 모든 운영 시간 삭제
	 * Bulk Delete 사용 → 영속성 컨텍스트 자동 동기화
	 *
	 * @param spaceId 공간 ID
	 * @return 삭제된 row 개수
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SpaceOperation o where o.space.spaceId = :spaceId")
	int deleteBySpaceId(@Param("spaceId") Integer spaceId);

	/**
	 * 특정 공간(SpaceId) 기준으로 모든 운영 시간 조회 (JPA 메서드 네이밍 버전)
	 *
	 * @param spaceId 공간 ID
	 * @return SpaceOperation 리스트
	 */
	List<SpaceOperation> findBySpace_SpaceId(Integer spaceId);
}
