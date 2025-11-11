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

	/**
	 * 지역 ID에 기반하여 승인자 리스트 조회 및 정렬
	 * 2차 승인자(role_id=1)의 adminRegion이 NULL일 경우를 대비해 LEFT JOIN FETCH 적용
	 * 1차 승인자(role_id=2)가 위로 오도록 roleId 내림차순 정렬
	 * 공간 관리에서 담당자 지정 시 사용
	 **/
	@Query("SELECT u FROM Admin u "
		+ "JOIN FETCH u.userRole ur "
		+ "LEFT JOIN FETCH u.adminRegion ar " // NULL 허용을 위해 LEFT JOIN FETCH 적용
		+ "WHERE ur.roleId = 1L OR (ur.roleId = 2L AND ar.regionId = :regionId) "
		+ "ORDER BY ur.roleId DESC")
	List<Admin> findApproversByRegion(@Param("regionId") Integer regionId);

	/**
	 * 특정 adminId로 Admin 정보를 조회하되, UserRole과 AdminRegion을 함께 로드 (Eager Loading)
	 * 공간 관리에서 담당자 지정 시 사용
	 **/
	@Query("SELECT u FROM Admin u "
		+ "JOIN FETCH u.userRole ur "
		+ "LEFT JOIN FETCH u.adminRegion ar " // adminRegion이 NULL일 수 있으므로 LEFT JOIN
		+ "WHERE u.adminId = :adminId")
	Optional<Admin> findAdminWithRoleAndRegion(@Param("adminId") Long adminId);
}
