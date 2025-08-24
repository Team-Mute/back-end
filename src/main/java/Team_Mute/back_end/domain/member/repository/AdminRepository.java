package Team_Mute.back_end.domain.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.entity.AdminRegion;
import Team_Mute.back_end.domain.member.entity.UserCompany;
import Team_Mute.back_end.domain.member.entity.UserRole;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

	boolean existsByAdminEmail(String adminEmail);

	Optional<Admin> findByAdminEmail(String adminEmail);

	@Query("SELECT u FROM Admin u " +
		"JOIN FETCH u.userRole " +
		"JOIN FETCH u.userCompany " +
		"JOIN FETCH u.adminRegion " +
		"WHERE u.adminEmail = :email")
	Optional<Admin> findAdminWithDetails(@Param("email") String email);

	List<Admin> findByUserCompany(UserCompany userCompany);

	List<Admin> findByUserRole(UserRole userRole);

	List<Admin> findByAdminRegion(AdminRegion adminRegion);

	@Query("select u.tokenVer from Admin u where u.adminId = :adminId")
	Optional<Integer> findTokenVerByAdminId(@Param("adminId") Long adminId);

	boolean existsByUserRole(UserRole userRole);

	List<Admin> findByAdminName(String adminName);
}
