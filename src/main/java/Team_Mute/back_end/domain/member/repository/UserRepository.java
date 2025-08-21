package Team_Mute.back_end.domain.member.repository;

import Team_Mute.back_end.domain.member.entity.AdminRegion;
import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.entity.UserCompany;
import Team_Mute.back_end.domain.member.entity.UserRole;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByUserEmail(String userEmail);

	Optional<User> findByUserEmail(String userEmail);

	@Query("SELECT u FROM User u " +
		"JOIN FETCH u.userRole " +
		"JOIN FETCH u.userCompany " +
		"JOIN FETCH u.adminRegion " +
		"WHERE u.userEmail = :email")
	Optional<User> findUserWithDetails(@Param("email") String email);

	List<User> findByUserCompany(UserCompany userCompany);

	List<User> findByUserRole(UserRole userRole);

	List<User> findByAdminRegion(AdminRegion adminRegion);

	@Query("select u.tokenVer from User u where u.userId = :userId")
	Optional<Integer> findTokenVerByUserId(@Param("userId") Long userId);

	boolean existsByUserRole(UserRole userRole);

	List<User> findByUserName(String userName);
}
