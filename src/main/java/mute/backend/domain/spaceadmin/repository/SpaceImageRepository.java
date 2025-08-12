package mute.backend.domain.spaceadmin.repository;

import java.util.List;
import mute.backend.domain.spaceadmin.entity.Space;
import mute.backend.domain.spaceadmin.entity.SpaceImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceImageRepository extends JpaRepository<SpaceImage, Integer> {
  void deleteBySpace(Space space);

  // Space 엔티티 기준으로 상세 이미지 목록 조회
  List<SpaceImage> findBySpace(Space space);
}
