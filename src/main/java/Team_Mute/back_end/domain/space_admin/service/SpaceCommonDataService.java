package Team_Mute.back_end.domain.space_admin.service;

import Team_Mute.back_end.domain.member.repository.AdminRegionRepository;
import Team_Mute.back_end.domain.space_admin.dto.CategoryListItem;
import Team_Mute.back_end.domain.space_admin.dto.LocationListItem;
import Team_Mute.back_end.domain.space_admin.dto.RegionListItem;
import Team_Mute.back_end.domain.space_admin.dto.TagListItem;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTag;
import Team_Mute.back_end.domain.space_admin.repository.SpaceCategoryRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceLocationRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceTagRepository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

	// 지역 전체 조회(공간 등록 및 수정할 시 사용)
	public List<RegionListItem> getAllRegions() {
		return regionRepository.findAll(Sort.by(Sort.Direction.ASC, "regionId"))
			.stream()
			.map(element -> new RegionListItem(element.getRegionId(), element.getRegionName()))
			.toList();
	}

	// 카테고리 전체 조회(공간 등록 및 수정할 시 사용)
	public List<CategoryListItem> getAllCategories() {
		return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryId"))
			.stream()
			.map(element -> new CategoryListItem(element.getCategoryId(), element.getCategoryName()))
			.toList();
	}

	// 태그 전체 조회(공간 등록 및 수정할 시 사용)
	public List<TagListItem> getAllTags() {
		return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "tagId"))
			.stream()
			.map(element -> new TagListItem(element.getTagId(), element.getTagName()))
			.toList();
	}

	// 지역 아이디로 주소 조회(공간 등록 및 수정할 시 사용)
	public List<LocationListItem> getLocationByRegionId(Integer regionId) {
		return spaceLocationRepository.findByAdminRegion_RegionIdAndIsActiveTrueOrderByLocationNameAsc(regionId).stream()
			.map(element -> new LocationListItem(
				element.getLocationId(),
				element.getLocationName(),
				element.getAddressRoad(),
				element.getPostalCode()
			)).toList();
	}

	// 태그(편의시설) 추가
	public SpaceTag createTag(String tagName) {
		// 중복 태그 검증
		if (tagRepository.findByTagName(tagName).isPresent()) {
			throw new IllegalArgumentException("이미 존재하는 태그입니다.");
		}

		SpaceTag newTag = SpaceTag.builder()
			.tagName(tagName)
			.regDate(LocalDateTime.now())
			.build();

		return tagRepository.save(newTag);
	}
}
