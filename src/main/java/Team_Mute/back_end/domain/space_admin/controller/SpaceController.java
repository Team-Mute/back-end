package Team_Mute.back_end.domain.space_admin.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import Team_Mute.back_end.domain.space_admin.dto.CategoryListItem;
import Team_Mute.back_end.domain.space_admin.dto.DeleteSpaceResponse;
import Team_Mute.back_end.domain.space_admin.dto.LocationListItem;
import Team_Mute.back_end.domain.space_admin.dto.RegionListItem;
import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateRequest;
import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateUpdateDoc;
import Team_Mute.back_end.domain.space_admin.dto.SpaceDatailResponse;
import Team_Mute.back_end.domain.space_admin.dto.SpaceListResponse;
import Team_Mute.back_end.domain.space_admin.dto.TagListItem;
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

@Tag(name = "공간 관리 API", description = "관리자 공간 관리 관련 API 명세")
@RestController
@RequestMapping("/api/spaces-admin")
@RequiredArgsConstructor
public class SpaceController {
	private final SpaceService spaceService;
	private final S3Uploader s3Uploader;

	// 지역 전체 조회(공간 등록 및 수정할 시 사용)
	@GetMapping("/regions")
	@Operation(summary = "지점 리스트 조회")
	public List<RegionListItem> getRegions() {
		return spaceService.getAllRegions();
	}

	// 카테고리 전체 조회(공간 등록 및 수정할 시 사용)
	@GetMapping("/categories")
	@Operation(summary = "카테고리 리스트 조회")
	public List<CategoryListItem> getCategories() {
		return spaceService.getAllCategories();
	}

	// 태그 전체 조회(공간 등록 및 수정할 시 사용)
	@GetMapping("/tags")
	@Operation(summary = "태그(편의시설) 조회")
	public List<TagListItem> getTags() {
		return spaceService.getAllTags();
	}

	// 지역 아이디로 건물 주소 조회
	@GetMapping("locations/{regionId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "조회할 지점 ID", required = true)
	@Operation(summary = "지점 아이디로 주소 조회")
	public List<LocationListItem> getLocationByRegionId(@PathVariable Integer regionId) {
		return spaceService.getLocationByRegionId(regionId);
	}

	// 공간 전체 조회
	@GetMapping
	@Operation(summary = "공간 리스트 조회")
	public List<SpaceListResponse> getAllSpaces() {
		return spaceService.getAllSpaces();
	}

	// 특정 공간 조회
	@GetMapping("/{spaceId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "조회할 공간 ID", required = true)
	@Operation(summary = "공간 단건 조회")
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
		)
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
		)
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
	@Operation(summary = "공간 삭제")
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
	@Operation(summary = "공간 복사")
	public ResponseEntity<SpaceDatailResponse> clone(@PathVariable Integer spaceId) {
		SpaceDatailResponse result = spaceService.cloneSpace(spaceId);
		return ResponseEntity.ok(result);
	}
}
