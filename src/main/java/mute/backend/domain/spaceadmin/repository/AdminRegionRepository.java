package mute.backend.domain.spaceadmin.repository;

import java.util.Optional;
import mute.backend.domain.spaceadmin.entity.AdminRegion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRegionRepository extends JpaRepository<AdminRegion, Integer> {
  Optional<AdminRegion> findByRegionName(String regionName);
}
