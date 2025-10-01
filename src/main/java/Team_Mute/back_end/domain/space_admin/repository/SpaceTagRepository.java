package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceTag;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * SpaceTag Repository
 * - tb_space_tags 테이블 매핑
 * - 공간 태그(예: WIFI, 빔프로젝터, 화이트보드 등) 관리
 * - 태그명으로 조회 기능 제공
 */
@Repository
public interface SpaceTagRepository extends JpaRepository<SpaceTag, Integer> {
	/**
	 * 태그명으로 SpaceTag 엔티티 조회
	 * - 예: "WIFI" 입력 시 해당 태그 엔티티 반환
	 */
	Optional<SpaceTag> findByTagName(String tagName);
}
