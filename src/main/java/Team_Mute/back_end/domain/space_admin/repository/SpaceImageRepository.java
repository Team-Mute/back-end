package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.entity.SpaceImage;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * SpaceCategory 엔티티 Repository
 * - tb_space_categories 테이블과 매핑
 * - 카테고리 정보(예: 미팅룸, 해사장 등)를 관리
 */
@Repository
public interface SpaceImageRepository extends JpaRepository<SpaceImage, Integer> {
	/**
	 * 특정 공간에 속한 모든 이미지 삭제
	 * - Space 엔티티 단위로 삭제 처리
	 *
	 * @param space Space 엔티티
	 */
	void deleteBySpace(Space space);

	/**
	 * 특정 공간의 상세 이미지 목록 조회
	 * - Space 엔티티 기준으로 fetch
	 *
	 * @param space Space 엔티티
	 * @return 해당 공간의 이미지 리스트
	 */
	List<SpaceImage> findBySpace(Space space);
}
