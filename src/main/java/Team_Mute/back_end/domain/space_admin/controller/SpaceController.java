package Team_Mute.back_end.domain.space_admin.controller;

import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateRequest;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.service.SpaceService;
import Team_Mute.back_end.domain.space_admin.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spaces-admin")
@RequiredArgsConstructor
public class SpaceController {

	private final SpaceService spaceService;
	private final S3Uploader s3Uploader;

	// 공간 전체 조회
	@GetMapping
	public List<Space> getSpaces() {
		return spaceService.getAllSpaces();
	}

	// 공간 등록 (이미지 여러 장 포함 - multipart/form-data)
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> upload(
		@RequestPart("space") @Valid SpaceCreateRequest request,
		@RequestPart("images") List<MultipartFile> images
	) {
		try {
			if (images == null || images.isEmpty()) {
				return ResponseEntity.badRequest().body("이미지가 최소 1장은 필요합니다.");
			}
			List<String> urls = s3Uploader.uploadAll(images, "spaces"); // throws IOException 버전
			Integer id = spaceService.createWithImages(request, urls);

			return ResponseEntity.status(201).body(Map.of("spaceId", id, "coverImageUrl", urls.get(0)));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("등록 실패: " + e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
		}
	}

}

