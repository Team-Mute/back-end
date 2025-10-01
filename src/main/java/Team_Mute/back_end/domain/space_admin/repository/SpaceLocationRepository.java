package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.SpaceLocation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * SpaceLocation Repository
 * - tb_space_locations 테이블 매핑
 * - 공간 위치(도로명 주소, 행정 구역 등) 관리
 */
@Repository
public interface SpaceLocationRepository extends JpaRepository<SpaceLocation, Integer> {

	/**
	 * LocationId(PK) 기준으로 단건 조회
	 *
	 * @param locationId 위치 고유 ID
	 * @return Optional<SpaceLocation>
	 */
	Optional<SpaceLocation> findByLocationId(Integer locationId);

	/**
	 * 특정 지역(regionId) 내에서 활성화된 위치 목록 조회
	 * - AdminRegion의 regionId 기준 필터링
	 * - isActive = true 조건
	 * - locationName 오름차순 정렬
	 *
	 * @param regionId 행정 구역 ID
	 * @return 활성화된 위치 리스트
	 */
	List<SpaceLocation> findByAdminRegion_RegionIdAndIsActiveTrueOrderByLocationNameAsc(Integer regionId);
}
