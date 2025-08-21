package Team_Mute.back_end.domain.space_user.controller;

import Team_Mute.back_end.domain.space_user.dto.SpaceUserResponseDto;
import Team_Mute.back_end.domain.space_user.service.SpaceUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 공간 검색 API", description = "사용자 공간 검색 관련 API 명세")
@RestController
@RequestMapping("/api/spaces-user")
@RequiredArgsConstructor
public class SpaceUserController {
	private final SpaceUserService spaceUserService;

	@GetMapping
	@Operation(summary = "공간 검색", description = "지역/카테고리/인원/편의시설(tagNames AND)로 필터링. 파라미터 없으면 전체")
	public ResponseEntity<List<SpaceUserResponseDto>> searchSpaces(
		@RequestParam(required = false) Integer regionId,     // CHANGE
		@RequestParam(required = false) Integer categoryId,   // CHANGE
		@RequestParam(required = false) Integer people,       // CHANGE
		@RequestParam(required = false) String[] tagNames     // CHANGE: 배열 파라미터
	) {
		return ResponseEntity.ok(
			spaceUserService.searchSpaces(regionId, categoryId, people, tagNames)
		);
	}
}
