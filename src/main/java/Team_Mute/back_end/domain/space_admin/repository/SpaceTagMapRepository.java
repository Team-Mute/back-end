package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTagMap;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * SpaceTagMap Repository
 * - tb_space_tag_map 테이블 매핑
 * - 공간(Space)과 태그(SpaceTag)의 다대다(M:N) 관계를 연결하는 매핑 엔티티 관리
 */
@Repository
public interface SpaceTagMapRepository extends JpaRepository<SpaceTagMap, Integer> {

	/**
	 * 특정 공간(Space)에 연결된 태그 매핑 전체 삭제
	 * - 공간이 삭제될 때 사용
	 * - @Transactional 적용으로 delete 연산 시 트랜잭션 보장
	 *
	 * @param space 삭제할 대상 공간 엔티티
	 */
	@Transactional
	void deleteBySpace(Space space);
}
