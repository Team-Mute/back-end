package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.AdminRegion;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRegionRepository extends JpaRepository<AdminRegion, Integer> {
	Optional<AdminRegion> findByRegionId(Integer regionId);
}
