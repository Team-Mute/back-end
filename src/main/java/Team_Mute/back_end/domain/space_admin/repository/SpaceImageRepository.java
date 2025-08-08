package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.entity.SpaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

//public interface SpaceImageRepository extends JpaRepository<SpaceImage, Integer> {
//	List<SpaceImage> findBySpace_SpaceIdOrderByImagePriorityAsc(Integer spaceId);
//}
public interface SpaceImageRepository extends JpaRepository<SpaceImage, Integer> {
	void deleteBySpace(Space space);
}
