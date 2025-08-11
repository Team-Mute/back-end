package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.entity.SpaceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceImageRepository extends JpaRepository<SpaceImage, Integer> {
	void deleteBySpace(Space space);

	// Space 엔티티 기준으로 상세 이미지 목록 조회
	List<SpaceImage> findBySpace(Space space);
}
