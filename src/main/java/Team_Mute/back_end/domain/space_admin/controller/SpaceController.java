package Team_Mute.back_end.domain.space_admin.controller;

import Team_Mute.back_end.domain.space_admin.dto.AdminRegionDto;
import Team_Mute.back_end.domain.space_admin.dto.DeleteSpaceResponse;
import Team_Mute.back_end.domain.space_admin.dto.PagedResponse;
import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateRequest;
import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateUpdateDoc;
import Team_Mute.back_end.domain.space_admin.dto.SpaceDatailResponse;
import Team_Mute.back_end.domain.space_admin.dto.SpaceListResponse;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTag;
import Team_Mute.back_end.domain.space_admin.repository.BoardRepository;
import Team_Mute.back_end.domain.space_admin.service.SpaceService;
import Team_Mute.back_end.domain.space_admin.util.S3Uploader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Tag(name = "공간 관리 API", description = "관리자 공간 관리 관련 API 명세")
@RestController
@RequestMapping("/api/spaces-admin")
@RequiredArgsConstructor
public class SpaceController {
	private final SpaceService spaceService;
	private final S3Uploader s3Uploader;
	private final BoardRepository boardRepository;

	// 관리자 담당 지역 조회
	@GetMapping("/regions")
	@Operation(summary = "관리자 담당 지역 조회", description = "토큰을 확인하여 담당 지역을 조회합니다.")
	public ResponseEntity<List<AdminRegionDto>> getAdminRegion(Authentication authentication) {
		Long adminId = Long.valueOf((String) authentication.getPrincipal());
		List<AdminRegionDto> regions = spaceService.getAdminRegion(adminId);
		return ResponseEntity.ok(regions);
	}

	// 공간 전체 조회 (페이징 적용)
	@GetMapping("/list")
	@Operation(summary = "공간 전체 조회", description = "토큰을 확인하여 공간 리스트를 조회합니다.")
	public ResponseEntity<PagedResponse<SpaceListResponse>> getAllSpaces(
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "6") int size) {

		// Pageable 객체 생성
		Pageable pageable = (Pageable) PageRequest.of(page, size);

		Page<SpaceListResponse> spacePage = spaceService.getAllSpaces(pageable);
		PagedResponse<SpaceListResponse> response = new PagedResponse<>(spacePage);

		return ResponseEntity.ok(response);
	}

	// 지역별 공간 조회
	@GetMapping("/list/{regionId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "조회할 지역 ID", required = true)
	@Operation(summary = "지역별 공간 리스트 조회", description = "토큰을 확인하여 지역별 공간 리스트를 조회합니다.")
	public List<SpaceListResponse> getAllSpacesByRegion(@PathVariable Integer regionId) {
		return spaceService.getAllSpacesByRegion(regionId);
	}

	// 특정 공간 조회
	@GetMapping("/detail/{spaceId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "조회할 공간 ID", required = true)
	@Operation(summary = "공간 단건 조회", description = "토큰을 확인하여 공간을 조회합니다.")
	public ResponseEntity<?> getSpaceById(@PathVariable Integer spaceId) {
		try {
			return ResponseEntity.ok(spaceService.getSpaceById(spaceId));
		} catch (NoSuchElementException e) {
			return ResponseEntity.status(404).body(java.util.Map.of("message", "공간을 찾을 수 없습니다."));
		}
	}

	private final ObjectMapper objectMapper; // ObjectMapper 주입

	// 공간 등록 (이미지 여러 장 포함 - multipart/form-data)
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
		summary = "공간 등록",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
				schema = @Schema(implementation = SpaceCreateUpdateDoc.class),
				encoding = {
					@Encoding(name = "space", contentType = MediaType.APPLICATION_JSON_VALUE),
					@Encoding(name = "images", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
				}
			)
		),
		description = "토큰을 확인하여 공간 등록을 진행합니다."
	)
	public ResponseEntity<?> upload(
		@RequestPart("space") String spaceJson,
		@RequestPart("images") List<MultipartFile> images
	) {
		try {
			// String 데이터를 SpaceCreateRequest 객체로 수동 변환
			SpaceCreateRequest request = objectMapper.readValue(spaceJson, SpaceCreateRequest.class);

			boolean noUsableFiles =
				(images == null || images.isEmpty()) || images.stream().allMatch(f -> f == null || f.isEmpty());
			if (noUsableFiles) {
				return ResponseEntity.badRequest().body("이미지는 최소 1장은 필요합니다.");
			}

			List<String> urls = s3Uploader.uploadAll(images, "spaces");
			Integer id = spaceService.createWithImages(request, urls);

			return ResponseEntity.ok(Map.of(
				"message", "등록 완료",
				"data", spaceService.getSpaceById(id)
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("등록 실패: " + e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
		}
	}

	// 공간 수정
	@PutMapping(value = "/{spaceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
		summary = "공간 수정",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
				schema = @Schema(implementation = SpaceCreateUpdateDoc.class),
				encoding = {
					@Encoding(name = "space", contentType = MediaType.APPLICATION_JSON_VALUE),
					@Encoding(name = "images", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
				}
			)
		),
		description = "토큰을 확인하여 공간 수정을 진행합니다."
	)
	public ResponseEntity<?> update(
		@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "수정할 공간 ID", required = true)
		@PathVariable Integer spaceId,
		@RequestPart("space") @Valid String spaceJson,
		@RequestPart(value = "images", required = false) List<MultipartFile> images
	) {
		try {
			// String 데이터를 SpaceCreateRequest 객체로 수동 변환
			SpaceCreateRequest request = objectMapper.readValue(spaceJson, SpaceCreateRequest.class);

			// 이미지가 없을 경우 예외 처리
			boolean noUsableFiles =
				(images == null || images.isEmpty()) || images.stream().allMatch(f -> f == null || f.isEmpty());

			if (noUsableFiles) {
				return ResponseEntity.badRequest().body("이미지는 최소 1장은 필요합니다.");
			}

			List<String> urls = (images != null && !images.isEmpty())
				? s3Uploader.uploadAll(images, "spaces")
				: null; // 이미지 변경 없으면 null

			spaceService.updateWithImages(spaceId, request, urls);

			return ResponseEntity.ok(Map.of(
				"message", "수정 완료",
				"data", spaceService.getSpaceById(spaceId)
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("수정 실패: " + e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
		}
	}

	// 공간 삭제
	@DeleteMapping("/{spaceId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "삭제할 공간 ID", required = true)
	@Operation(summary = "공간 삭제", description = "토큰을 확인하여 공간 삭제를 진행합니다.")
	public ResponseEntity<DeleteSpaceResponse> delete(@PathVariable Integer spaceId) {
		spaceService.deleteSpace(spaceId);
		return ResponseEntity.ok(new DeleteSpaceResponse(
			"공간 삭제 완료",
			spaceId
		));
	}

	// 공간 복사 (기존 공간을 기준으로 새 공간 생성)
	@PostMapping("/copy/{spaceId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "복사할 공간 ID", required = true)
	@Operation(summary = "공간 복사", description = "토큰을 확인하여 공간 복사를 진행합니다.")
	public ResponseEntity<SpaceDatailResponse> clone(@PathVariable Integer spaceId) {
		SpaceDatailResponse result = spaceService.cloneSpace(spaceId);
		return ResponseEntity.ok(result);
	}

	// 태그(편의시설) 추가
	@PostMapping("/tags")
	@Operation(summary = "태그(편의시설) 등록", description = "토큰을 확인하여 편의시설을 등록합니다.")
	public ResponseEntity<?> createTag(@RequestParam String tagName) {
		// 입력 파라미터 유효성 검사
		if (tagName == null || tagName.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		try {
			SpaceTag createdTag = spaceService.createTag(tagName);
			return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
		} catch (IllegalArgumentException e) {
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Conflict");
			errorResponse.put("message", "이미 존재하는 태그입니다.");

			return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
		}
	}
}
