package Team_Mute.back_end.domain.space_admin.controller;

import Team_Mute.back_end.domain.space_admin.dto.CategoryListItem;
import Team_Mute.back_end.domain.space_admin.dto.LocationListItem;
import Team_Mute.back_end.domain.space_admin.dto.RegionListItem;
import Team_Mute.back_end.domain.space_admin.dto.TagListItem;
import Team_Mute.back_end.domain.space_admin.service.SpaceCommonDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공간 관련 공통 API", description = "공간 관련 공통 API 명세")
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceCommonDataController {
	private final SpaceCommonDataService spaceCommonDataService;

	// 지역 전체 조회(공간 등록 및 수정할 시 사용)
	@GetMapping("/regions")
	@Operation(summary = "지점 리스트 조회")
	public List<RegionListItem> getRegions() {
		return spaceCommonDataService.getAllRegions();
	}

	// 카테고리 전체 조회(공간 등록 및 수정할 시 사용)
	@GetMapping("/categories")
	@Operation(summary = "카테고리 리스트 조회")
	public List<CategoryListItem> getCategories() {
		return spaceCommonDataService.getAllCategories();
	}

	// 태그 전체 조회(공간 등록 및 수정할 시 사용)
	@GetMapping("/tags")
	@Operation(summary = "태그(편의시설) 조회")
	public List<TagListItem> getTags() {
		return spaceCommonDataService.getAllTags();
	}

	// 지역 아이디로 건물 주소 조회
	@GetMapping("locations/{regionId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "조회할 지점 ID", required = true)
	@Operation(summary = "지점 아이디로 주소 조회")
	public List<LocationListItem> getLocationByRegionId(@PathVariable Integer regionId) {
		return spaceCommonDataService.getLocationByRegionId(regionId);
	}
}
