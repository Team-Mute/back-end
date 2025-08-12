package mute.backend.domain.spaceadmin.repository;

import mute.backend.domain.spaceadmin.entity.Space;
import mute.backend.domain.spaceadmin.entity.SpaceTagMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SpaceTagMapRepository extends JpaRepository<SpaceTagMap, Integer> {

  @Transactional
  void deleteBySpace(Space space);
}
