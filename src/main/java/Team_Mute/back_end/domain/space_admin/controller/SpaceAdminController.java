package Team_Mute.back_end.domain.space_admin.controller;

import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateUpdateDoc;
import Team_Mute.back_end.domain.space_admin.dto.request.SpaceCreateRequestDto;
import Team_Mute.back_end.domain.space_admin.dto.response.DeleteSpaceResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.PagedResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.SpaceListResponseDto;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTag;
import Team_Mute.back_end.domain.space_admin.repository.BoardRepository;
import Team_Mute.back_end.domain.space_admin.service.SpaceAdminService;
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
public class SpaceAdminController {
	private final SpaceAdminService spaceAdminService;
	private final S3Uploader s3Uploader;
	private final BoardRepository boardRepository;

	/**
	 * 공간 전체 조회 (페이징 적용)
	 **/
	@GetMapping("/list")
	@Operation(summary = "공간 전체 조회", description = "토큰을 확인하여 공간 리스트를 조회합니다.")
	public ResponseEntity<PagedResponseDto<SpaceListResponseDto>> getAllSpaces(
		Authentication authentication,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "6") int size) {

		// 1부터 시작하는 페이지 요청을 Spring Data JPA의 0부터 시작하는 페이지로 변환
		int adjustedPage = Math.max(0, page - 1);

		// Pageable 객체 생성
		Pageable pageable = PageRequest.of(adjustedPage, size);
		Long adminId = Long.valueOf((String) authentication.getPrincipal());

		Page<SpaceListResponseDto> spacePage = spaceAdminService.getAllSpaces(pageable, adminId);
		PagedResponseDto<SpaceListResponseDto> response = new PagedResponseDto<>(spacePage);

		return ResponseEntity.ok(response);
	}

	/**
	 * 지역별 공간 조회
	 **/
	@GetMapping("/list/{regionId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "조회할 지역 ID", required = true)
	@Operation(summary = "지역별 공간 리스트 조회", description = "토큰을 확인하여 지역별 공간 리스트를 조회합니다.")
	public List<SpaceListResponseDto> getAllSpacesByRegion(@PathVariable Integer regionId) {
		return spaceAdminService.getAllSpacesByRegion(regionId);
	}

	/**
	 * 특정 공간 조회
	 **/
	@GetMapping("/detail/{spaceId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "조회할 공간 ID", required = true)
	@Operation(summary = "공간 단건 조회", description = "토큰을 확인하여 공간을 조회합니다.")
	public ResponseEntity<?> getSpaceById(@PathVariable Integer spaceId) {
		try {
			return ResponseEntity.ok(spaceAdminService.getSpaceById(spaceId));
		} catch (NoSuchElementException e) {
			return ResponseEntity.status(404).body(java.util.Map.of("message", "공간을 찾을 수 없습니다."));
		}
	}

	private final ObjectMapper objectMapper; // ObjectMapper 주입

	/**
	 * 공간 등록 (이미지 여러 장 포함 - multipart/form-data)
	 **/
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
		description = "토큰을 확인하여 공간 등록을 진행합니다.(이미지는 최소 1장, 최대 5장)"
	)
	public ResponseEntity<?> upload(
		@RequestPart("space") String spaceJson,
		@RequestPart("images") List<MultipartFile> images
	) {
		try {
			// String 데이터를 SpaceCreateRequest 객체로 수동 변환
			SpaceCreateRequestDto request = objectMapper.readValue(spaceJson, SpaceCreateRequestDto.class);

			// 업로드 가능한 파일(= null 아니고 비어있지 않은 파일)만 필터링
			List<MultipartFile> usableImages = (images == null) ? List.of()
				: images.stream()
				.filter(f -> f != null && !f.isEmpty())
				.toList();

			// 최소/최대 개수 검증
			if (usableImages.isEmpty()) {
				return ResponseEntity.badRequest().body("이미지는 최소 1장은 필요합니다.");
			}
			if (usableImages.size() > 5) {
				return ResponseEntity.badRequest().body("이미지는 최대 5장까지만 업로드할 수 있습니다.");
			}

			// 이미지를 'temp' 폴더에 먼저 업로드
			List<String> tempUrls = s3Uploader.uploadAll(images, "temp");
			Integer id = spaceAdminService.createWithImages(request, tempUrls);

			return ResponseEntity.ok(Map.of(
				"message", "등록 완료",
				"data", spaceAdminService.getSpaceById(id)
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("등록 실패: " + e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
		}
	}

	/**
	 * 공간 수정
	 **/
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
		description = "토큰을 확인하여 공간 수정을 진행합니다.(이미지는 최소 1장, 최대 5장)"
	)
	public ResponseEntity<?> update(
		@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "수정할 공간 ID", required = true)
		@PathVariable Integer spaceId,
		@RequestPart("space") @Valid String spaceJson,
		@RequestPart(value = "images", required = false) List<MultipartFile> images
	) {
		try {
			// String 데이터를 SpaceCreateRequest 객체로 수동 변환
			SpaceCreateRequestDto request = objectMapper.readValue(spaceJson, SpaceCreateRequestDto.class);

			// 업로드 가능한 파일(= null 아니고 비어있지 않은 파일)만 필터링
			List<MultipartFile> usableImages = (images == null) ? List.of()
				: images.stream()
				.filter(f -> f != null && !f.isEmpty())
				.toList();

			// 최소/최대 개수 검증
			if (usableImages.isEmpty()) {
				return ResponseEntity.badRequest().body("이미지는 최소 1장은 필요합니다.");
			}
			if (usableImages.size() > 5) {
				return ResponseEntity.badRequest().body("이미지는 최대 5장까지만 업로드할 수 있습니다.");
			}

			List<String> urls = (images != null && !images.isEmpty())
				? s3Uploader.uploadAll(images, "spaces/" + spaceId)
				: null; // 이미지 변경 없으면 null

			spaceAdminService.updateWithImages(spaceId, request, urls);

			return ResponseEntity.ok(Map.of(
				"message", "수정 완료",
				"data", spaceAdminService.getSpaceById(spaceId)
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("수정 실패: " + e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
		}
	}

	/**
	 * 공간 삭제
	 **/
	@DeleteMapping("/{spaceId}")
	@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "삭제할 공간 ID", required = true)
	@Operation(summary = "공간 삭제", description = "토큰을 확인하여 공간 삭제를 진행합니다.")
	public ResponseEntity<DeleteSpaceResponseDto> delete(@PathVariable Integer spaceId) {
		spaceAdminService.deleteSpace(spaceId);
		return ResponseEntity.ok(new DeleteSpaceResponseDto(
			"공간 삭제 완료",
			spaceId
		));
	}

	/**
	 * 태그(편의시설) 추가
	 **/
	@PostMapping("/tags")
	@Operation(summary = "태그(편의시설) 등록", description = "토큰을 확인하여 편의시설을 등록합니다.")
	public ResponseEntity<?> createTag(@RequestParam String tagName) {
		// 입력 파라미터 유효성 검사
		if (tagName == null || tagName.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		try {
			SpaceTag createdTag = spaceAdminService.createTag(tagName);
			return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
		} catch (IllegalArgumentException e) {
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Conflict");
			errorResponse.put("message", "이미 존재하는 태그입니다.");

			return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
		}
	}
}
