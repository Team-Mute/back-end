package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceCategory;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * SpaceCategory 엔티티 Repository
 * - tb_space_categories 테이블과 매핑
 * - 카테고리 정보(예: 미팅룸, 세미나실 등)를 관리
 */
@Repository
public interface SpaceCategoryRepository extends JpaRepository<SpaceCategory, Integer> {
	/**
	 * 카테고리 ID로 카테고리 조회
	 *
	 * @param categoryId 카테고리 고유 ID (PK)
	 * @return Optional<SpaceCategory> 조회된 카테고리 (없을 경우 empty)
	 */
	Optional<SpaceCategory> findByCategoryId(Integer categoryId);
}
