package Team_Mute.back_end.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.member.entity.UserCompany;

@Repository
public interface UserCompanyRepository extends JpaRepository<UserCompany, Integer> {

	Optional<UserCompany> findByCompanyName(String companyName);

	boolean existsByCompanyName(String companyName);

	@Query("SELECT MAX(uc.companyId) FROM UserCompany uc")
	Optional<Integer> findMaxCompanyId();
}

