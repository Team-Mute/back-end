package Team_Mute.back_end.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.member.entity.UserRole;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

	Optional<UserRole> findByRoleName(String roleName);

	boolean existsByRoleName(String roleName);
}
