package Team_Mute.back_end.domain.space_admin.repository;


import Team_Mute.back_end.domain.space_admin.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceRepository extends JpaRepository<Space, Integer> {
}
