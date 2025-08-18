package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceLocation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpaceLocationRepository extends JpaRepository<SpaceLocation, Integer> {

    Optional<SpaceLocation> findByLocationId(Integer regionId);

    List<SpaceLocation> findByRegionIdAndIsActiveTrueOrderByLocationNameAsc(Integer regionId);
}
