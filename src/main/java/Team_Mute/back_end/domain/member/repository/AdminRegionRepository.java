package Team_Mute.back_end.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.member.entity.AdminRegion;

@Repository
public interface AdminRegionRepository extends JpaRepository<AdminRegion, Integer> {

	Optional<AdminRegion> findByRegionName(String regionName);

	Optional<AdminRegion> findByRegionId(Integer regionId);

	boolean existsByRegionName(String regionName);
}
