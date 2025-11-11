package Team_Mute.back_end.domain.space_admin.controller;

import Team_Mute.back_end.domain.space_admin.dto.response.CategoryListResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.LocationListResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.RegionListResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.TagListResponseDto;
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

/**
 * 공간 공통 데이터 Controller
 * - 로그인하지 않아도 누구나 접근 가능한 공용 API 제공
 * - 토큰 검증이나 권한 체크를 하지 않음
 * - 공간 카테고리, 태그 등 공통 데이터 조회에 사용
 */
@Tag(name = "공간 관련 공통 API", description = "공간 관련 공통 API 명세")
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceCommonDataController {
	private final SpaceCommonDataService spaceCommonDataService;

	/**
	 * 지역 전체 조회
	 *
	 * @return 지역 ID와 이름을 포함하는 DTO 리스트
	 **/
	@GetMapping("/regions")
	@Operation(summary = "지역 리스트 조회", description = "인증 없이 지역 리스트를 조회합니다.")
	public List<RegionListResponseDto> getRegions() {
		return spaceCommonDataService.getAllRegions();
	}

	/**
	 * 태그(편의시설) 전체 조회
	 *
	 * @return 태그 ID와 이름을 포함하는 DTO 리스트
	 **/
	@GetMapping("/tags")
	@Operation(summary = "태그(편의시설) 조회", description = "인증 없이 태그 리스트를 조회합니다.")
	public List<TagListResponseDto> getTags() {
		return spaceCommonDataService.getAllTags();
	}


	/**
	 * 카테고리 전체 조회
	 *
	 * @return 카테고리 ID와 이름을 포함하는 DTO 리스트
	 **/
	@GetMapping("/categories")
	@Operation(summary = "카테고리 리스트 조회", description = "토큰을 확인하여 카테고리 리스트를 조회합니다.")
	public List<CategoryListResponseDto> getCategories() {
		return spaceCommonDataService.getAllCategories();
	}

	/**
	 * 지역 아이디로 건물 주소 조회
	 *
	 * @param regionId 조회할 지역 ID (Path Variable)
	 * @return 주소 ID, 이름, 주소(도로명+지번 조합) 등을 포함하는 DTO 리스트
	 **/
	@GetMapping("locations/{regionId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "조회할 지점 ID", required = true)
	@Operation(summary = "지역 아이디로 주소 조회", description = "토큰을 확인하여 주소를 조회합니다.")
	public List<LocationListResponseDto> getLocationByRegionId(@PathVariable Integer regionId) {
		return spaceCommonDataService.getLocationByRegionId(regionId);
	}
}
