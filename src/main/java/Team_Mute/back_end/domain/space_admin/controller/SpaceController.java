package Team_Mute.back_end.domain.space_admin.controller;

import Team_Mute.back_end.domain.space_admin.dto.CategoryListItem;
import Team_Mute.back_end.domain.space_admin.dto.DeleteSpaceResponse;
import Team_Mute.back_end.domain.space_admin.dto.LocationListItem;
import Team_Mute.back_end.domain.space_admin.dto.RegionListItem;
import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateRequest;
import Team_Mute.back_end.domain.space_admin.dto.SpaceListResponse;
import Team_Mute.back_end.domain.space_admin.dto.TagListItem;
import Team_Mute.back_end.domain.space_admin.service.SpaceService;
import Team_Mute.back_end.domain.space_admin.util.S3Uploader;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

@RestController
@RequestMapping("/api/spaces-admin")
@RequiredArgsConstructor
public class SpaceController {

	private final SpaceService spaceService;
	private final S3Uploader s3Uploader;

	// 지역 전체 조회(공간 등록 및 수정할 시 사용)
	@GetMapping("/regions")
	public List<RegionListItem> getRegions() {
		return spaceService.getAllRegions();
	}

	// 카테고리 전체 조회(공간 등록 및 수정할 시 사용)
	@GetMapping("/categories")
	public List<CategoryListItem> getCategories() {
		return spaceService.getAllCategories();
	}

	// 태그 전체 조회(공간 등록 및 수정할 시 사용)
	@GetMapping("/tags")
	public List<TagListItem> getTags() {
		return spaceService.getAllTags();
	}

	// 지역 아이디로 건물 주소 조회
	@GetMapping("locations/{regionId}")
	public List<LocationListItem> getLocationByRegionId(@PathVariable Integer regionId) {
		return spaceService.getLocationByRegionId(regionId);
	}

	// 공간 전체 조회
	@GetMapping
	public List<SpaceListResponse> getAllSpaces() {
		return spaceService.getAllSpaces();
	}

	// 특정 공간 조회
	@GetMapping("/{spaceId}")
	public ResponseEntity<?> getSpaceById(@PathVariable Integer spaceId) {
		try {
			return ResponseEntity.ok(spaceService.getSpaceById(spaceId));
		} catch (NoSuchElementException e) {
			return ResponseEntity.status(404).body(java.util.Map.of("message", "공간을 찾을 수 없습니다."));
		}
	}

	// 공간 등록 (이미지 여러 장 포함 - multipart/form-data)
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> upload(
		@RequestPart("space") @Valid SpaceCreateRequest request,
		@RequestPart("images") List<MultipartFile> images
	) {
		try {
			// 이미지가 없을 경우 예외 처리
			boolean noUsableFiles = (images == null || images.isEmpty()) || images.stream().allMatch(f -> f == null || f.isEmpty());
			if (noUsableFiles) {
				return ResponseEntity.badRequest().body("이미지는 최소 1장은 필요합니다.");
			}

			List<String> urls = s3Uploader.uploadAll(images, "spaces"); // throws IOException 버전
			Integer id = spaceService.createWithImages(request, urls);

			String message = "";
			if (request.getSaveStatus().equals("DRAFT")) {
				message = "임시 저장 완료";
			} else if (request.getSaveStatus().equals("PUBLISHED")) {
				message = "등록 완료";
			}
			return ResponseEntity.ok(Map.of(
				"message", message,
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
	public ResponseEntity<?> update(
		@PathVariable Integer spaceId,
		@RequestPart("space") @Valid SpaceCreateRequest request,
		@RequestPart(value = "images", required = false) List<MultipartFile> images
	) {
		try {
			// 이미지가 없을 경우 예외 처리
			boolean noUsableFiles = (images == null || images.isEmpty()) || images.stream().allMatch(f -> f == null || f.isEmpty());

			if (noUsableFiles) {
				return ResponseEntity.badRequest().body("이미지는 최소 1장은 필요합니다.");
			}

			List<String> urls = (images != null && !images.isEmpty())
				? s3Uploader.uploadAll(images, "spaces")
				: null; // 이미지 변경 없으면 null

			spaceService.updateWithImages(spaceId, request, urls);

			String message = "";
			if (request.getSaveStatus().equals("DRAFT")) {
				message = "임시 저장 수정 완료";
			} else if (request.getSaveStatus().equals("PUBLISHED")) {
				message = "수정 완료";
			}
			return ResponseEntity.ok(Map.of(
				"message", message,
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
	public ResponseEntity<DeleteSpaceResponse> delete(@PathVariable Integer spaceId) {
		spaceService.deleteSpace(spaceId);
		return ResponseEntity.ok(new DeleteSpaceResponse(
			"공간 삭제 완료",
			spaceId
		));
	}

	// 공간 복제 (기존 공간을 기준으로 새 공간 생성)
	@PostMapping("/clone/{spaceId}")
	public ResponseEntity<SpaceListResponse> clone(@PathVariable Integer spaceId) {
		SpaceListResponse result = spaceService.cloneSpace(spaceId);
		return ResponseEntity.ok(result);
	}
}

