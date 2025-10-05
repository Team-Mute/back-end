package Team_Mute.back_end.domain.space_user.controller;

import Team_Mute.back_end.domain.space_user.dto.SpaceSearchResponse;
import Team_Mute.back_end.domain.space_user.service.SpaceUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 공간 검색 API", description = "사용자 공간 검색 관련 API 명세")
@RestController
@RequestMapping("/api/spaces-user")
@RequiredArgsConstructor
public class SpaceUserController {
	private final SpaceUserService spaceUserService;

	// 공간 검색
	@GetMapping
	@Operation(summary = "공간 검색", description = "지역(regionId)/인원(people)/편의시설(tagNames[])로 필터링. 파라미터 없으면 전체")
	public ResponseEntity<SpaceSearchResponse> searchSpaces(
		@RequestParam(required = false) Integer regionId,
		@RequestParam(required = false) Integer people,
		@RequestParam(required = false) String[] tagNames,
		@RequestParam(name = "startDateTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
		@RequestParam(name = "endDateTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime
	) {
		return ResponseEntity.ok(
			spaceUserService.searchSpaces(regionId, people, tagNames, startDateTime, endDateTime)
		);
	}

	// 특정 공간 상세 정보 조회
	@GetMapping("/detail/{spaceId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "조회할 공간 ID", required = true)
	@Operation(summary = "공간 단건 조회", description = "공간 아이디를 입력하여 공간을 조회합니다.")
	public ResponseEntity<?> getSpaceById(@PathVariable Integer spaceId) {
		try {
			return ResponseEntity.ok(spaceUserService.getSpaceById(spaceId));
		} catch (NoSuchElementException e) {
			return ResponseEntity.status(404).body(java.util.Map.of("message", "공간을 찾을 수 없습니다."));
		}
	}
}
