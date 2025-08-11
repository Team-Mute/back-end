package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.AdminRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRegionRepository extends JpaRepository<AdminRegion, Integer> {
	Optional<AdminRegion> findByRegionName(String regionName);
}
