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

/**
 * 사용자 공간 검색 및 상세 정보 조회 요청을 처리하는 컨트롤러
 * 지역, 인원, 태그, 예약 가능 시간 등을 기준으로 공간을 검색하는 기능을 제공
 * Lombok의 {@code @RequiredArgsConstructor}를 사용하여 {@code SpaceUserService}를 주입
 */
@Tag(name = "사용자 공간 검색 API", description = "사용자 공간 검색 관련 API 명세")
@RestController
@RequestMapping("/api/spaces-user")
@RequiredArgsConstructor
public class SpaceUserController {
	private final SpaceUserService spaceUserService;

	/**
	 * 공간 검색 API
	 * - 지역, 인원수, 태그, 예약 시작/종료 시간을 기준으로 공간을 필터링하여 반환
	 * - 모든 파라미터가 없을 경우, 사용 가능한 전체 공간 리스트를 반환
	 *
	 * @param regionId      지역 ID (선택 사항)
	 * @param people        인원수 (선택 사항)
	 * @param tagNames      편의시설 태그 목록 (선택 사항)
	 * @param startDateTime 예약 시작 시간 (선택 사항, ISO 8601 형식)
	 * @param endDateTime   예약 종료 시간 (선택 사항, ISO 8601 형식)
	 * @return 미팅룸과 이벤트홀로 분류된 검색 결과를 담은 {@code SpaceSearchResponse}
	 */
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

	/**
	 * 특정 공간의 상세 정보를 조회하는 API
	 *
	 * @param spaceId 조회할 공간의 고유 ID
	 * @return 공간 상세 정보를 담은 {@code SpaceUserDtailResponseDto} 또는 404 에러 메시지
	 */
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
