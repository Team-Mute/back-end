package Team_Mute.back_end.domain.space_admin.service;

import Team_Mute.back_end.domain.member.repository.AdminRegionRepository;
import Team_Mute.back_end.domain.space_admin.dto.response.CategoryListResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.LocationListResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.RegionListResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.TagListResponseDto;
import Team_Mute.back_end.domain.space_admin.repository.SpaceCategoryRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceLocationRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceTagRepository;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 공간 공통 데이터 Service
 * - 로그인하지 않아도 누구나 사용할 수 있는 공용 비즈니스 로직 제공
 * - 지역, 카테고리, 태그 등 공통 데이터를 조회하는 기능 담당
 * - 캐싱 전략을 적용할 수 있는 후보 지점 (조회 성능 최적화 고려)
 */
@Service
public class SpaceCommonDataService {
	private final SpaceRepository spaceRepository;
	private final SpaceCategoryRepository categoryRepository;
	private final AdminRegionRepository regionRepository;
	private final SpaceTagRepository tagRepository;
	private final SpaceLocationRepository spaceLocationRepository;

	public SpaceCommonDataService(
		SpaceRepository spaceRepository,
		SpaceCategoryRepository categoryRepository,
		AdminRegionRepository regionRepository,
		SpaceTagRepository tagRepository,
		SpaceLocationRepository spaceLocationRepository
	) {
		this.spaceRepository = spaceRepository;
		this.categoryRepository = categoryRepository;
		this.regionRepository = regionRepository;
		this.tagRepository = tagRepository;
		this.spaceLocationRepository = spaceLocationRepository;
	}

	/**
	 * 지역 전체 조회
	 **/
	public List<RegionListResponseDto> getAllRegions() {
		return regionRepository.findAll(Sort.by(Sort.Direction.ASC, "regionId"))
			.stream()
			.map(element -> new RegionListResponseDto(element.getRegionId(), element.getRegionName()))
			.toList();
	}

	/**
	 * 카테고리 전체 조회
	 **/
	public List<CategoryListResponseDto> getAllCategories() {
		return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryId"))
			.stream()
			.map(element -> new CategoryListResponseDto(element.getCategoryId(), element.getCategoryName()))
			.toList();
	}

	/**
	 * 태그 전체 조회
	 **/
	public List<TagListResponseDto> getAllTags() {
		return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "tagId"))
			.stream()
			.map(element -> new TagListResponseDto(element.getTagId(), element.getTagName()))
			.toList();
	}

	/**
	 * 지역 아이디로 주소 조회
	 **/
	public List<LocationListResponseDto> getLocationByRegionId(Integer regionId) {
		return spaceLocationRepository.findByAdminRegion_RegionIdAndIsActiveTrueOrderByLocationNameAsc(regionId).stream()
			.map(element -> {

				// 1. 도로명 주소와 지번 주소를 조합합니다.
				// 결과: "서울특별시 명동10길 52 (충무로2가 65-4)"
				String combinedAddressRoad = String.format(
					"%s (%s)",
					element.getAddressRoad(),
					element.getAddressJibun()
				);

				// 2. Record DTO를 생성하며, 필드 순서에 맞게 정확히 매핑합니다.
				return new LocationListResponseDto(
					// 1. locationId
					element.getLocationId(),

					// 2. locationName
					element.getLocationName(),

					// 3. addressRoad (조합된 주소)
					combinedAddressRoad,

					// 4. postalCode (우편번호)
					element.getPostalCode(),

					// 5. addressInfo (접근성 정보)
					element.getAccessInfo()
				);
			}).toList();
	}
}
