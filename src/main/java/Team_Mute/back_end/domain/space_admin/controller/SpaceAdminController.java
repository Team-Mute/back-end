package Team_Mute.back_end.domain.space_admin.controller;

import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateDoc;
import Team_Mute.back_end.domain.space_admin.dto.SpaceUpdateDoc;
import Team_Mute.back_end.domain.space_admin.dto.request.SpaceCreateRequestDto;
import Team_Mute.back_end.domain.space_admin.dto.response.AdminListResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.DeleteSpaceResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.SpaceListResponseDto;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTag;
import Team_Mute.back_end.domain.space_admin.service.SpaceAdminService;
import Team_Mute.back_end.domain.space_admin.util.S3Deleter;
import Team_Mute.back_end.domain.space_admin.util.S3Uploader;
import Team_Mute.back_end.global.dto.PagedResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * 공간 관리 Controller
 * - 관리자 페이지의 '공간 관리' 메뉴에서 사용하는 API 집합
 * - 공간 등록/수정/삭제/조회 기능을 제공
 * - 카테고리와 태그 같은 부가 정보 처리도 포함
 * - 모든 요청은 관리자 로그인 필요
 * - 공간 등록/수정/삭제 시 트랜잭션 단위로 처리되어 원자성을 보장
 */
@Slf4j
@Tag(name = "공간 관리 API", description = "관리자 공간 관리 관련 API 명세")
@RestController
@RequestMapping("/api/spaces-admin")
@RequiredArgsConstructor
public class SpaceAdminController {
	private final SpaceAdminService spaceAdminService;
	private final S3Uploader s3Uploader;
	private final S3Deleter s3Deleter;

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
	 * 특정 공간 상세 조회
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
	@Autowired
	private Validator validator;

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
				schema = @Schema(implementation = SpaceCreateDoc.class),
				encoding = {
					@Encoding(name = "space", contentType = MediaType.APPLICATION_JSON_VALUE),
					@Encoding(name = "images", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
				}
			)
		),
		description = "토큰을 확인하여 공간 등록을 진행합니다.(이미지는 최소 1장, 최대 5장)"
	)
	public ResponseEntity<?> upload(
		Authentication authentication,
		@RequestPart("space") String spaceJson,
		@RequestPart("images") List<MultipartFile> images
	) {
		try {
			// 관리자 권한 아이디
			Long adminId = Long.valueOf((String) authentication.getPrincipal());

			// String 데이터를 SpaceCreateRequest 객체로 수동 변환
			SpaceCreateRequestDto request = objectMapper.readValue(spaceJson, SpaceCreateRequestDto.class);

			// DTO에 대한 수동 유효성 검증 실행
			Set<ConstraintViolation<SpaceCreateRequestDto>> violations = validator.validate(request);

			if (!violations.isEmpty()) {
				// 유효성 검증 실패 시, 400 Bad Request와 에러 메시지 반환
				String errorMessage = violations.iterator().next().getMessage();
				return ResponseEntity.badRequest().body("입력 오류: " + errorMessage);
			}

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
			Integer id = spaceAdminService.createWithImages(adminId, request, tempUrls);

			// 모든 처리가 성공적으로 완료된 후, 임시 폴더를 삭제
			try {
				s3Deleter.deleteFolder("temp");
			} catch (Exception e) {
				// 삭제 실패 시 로그를 남기지만, 전체 프로세스를 중단시키지는 않습니다.
				// 메인 작업(공간 등록)은 이미 성공했기 때문입니다.
				System.err.println("임시 폴더 삭제 실패: " + e.getMessage());
			}

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
				schema = @Schema(implementation = SpaceUpdateDoc.class),
				encoding = {
					@Encoding(name = "space", contentType = MediaType.APPLICATION_JSON_VALUE),
					@Encoding(name = "images", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE),
					@Encoding(name = "keepUrlsOrder", contentType = MediaType.APPLICATION_JSON_VALUE) // 유지할 기존 이미지 URL 목록 + 순서
				}
			)
		),
		description = "토큰을 확인하여 공간 수정을 진행합니다.(이미지는 최소 1장, 최대 5장)"
	)
	public ResponseEntity<?> update(
		Authentication authentication,
		@Parameter(name = "spaceId", in = ParameterIn.PATH, description = "수정할 공간 ID", required = true)
		@PathVariable Integer spaceId,
		@RequestPart("space") @Valid String spaceJson,
		@RequestPart(value = "images", required = false) List<MultipartFile> images,
		@RequestPart(value = "keepUrlsOrder", required = false) List<String> keepUrlsOrder // 유지할 기존 이미지 URL 목록 + 순서
	) {
		try {
			// 관리자 권한 아이디
			Long adminId = Long.valueOf((String) authentication.getPrincipal());

			// 0) DTO 파싱
			SpaceCreateRequestDto request = objectMapper.readValue(spaceJson, SpaceCreateRequestDto.class);

			// 1) 파일 필터링
			List<MultipartFile> usableImages = (images == null) ? List.of()
				: images.stream().filter(f -> f != null && !f.isEmpty()).toList();

			// 2) 신규 업로드
			List<String> uploadedUrls = usableImages.isEmpty()
				? List.of()
				: s3Uploader.uploadAll(usableImages, "spaces/" + spaceId);

			// 3) keepUrlsOrder 필수 사용 (없으면 400)
			if (keepUrlsOrder == null) {
				return ResponseEntity.badRequest().body(
					"keepUrlsOrder는 필수입니다. 최종 순서를 JSON 배열로 보내주세요. (예: [\"기존URL\",\"new:0\",\"new:1\"])");
			}

			List<String> finalUrls = new ArrayList<>();

			// 3-1) 빈 배열([])이면 전체 삭제 의도로 간주
			if (keepUrlsOrder.isEmpty()) {
				if (!uploadedUrls.isEmpty()) {
					return ResponseEntity.badRequest().body(
						"keepUrlsOrder가 빈 배열인데 새 이미지가 첨부되었습니다. new:i 토큰으로 순서를 명시하세요.");
				}
				// 전부 삭제 → 최종 목록은 빈 리스트
			} else {
				// 3-2) "new:i" 토큰 검증
				Pattern p = Pattern.compile("^new:(\\d+)$");
				Set<Integer> tokenIdx = new LinkedHashSet<>();

				for (String item : keepUrlsOrder) {
					if (item == null || item.isBlank()) continue;
					Matcher m = p.matcher(item);
					if (m.matches()) tokenIdx.add(Integer.parseInt(m.group(1)));
				}

				if (uploadedUrls.isEmpty()) {
					if (!tokenIdx.isEmpty()) {
						return ResponseEntity.badRequest().body("새 이미지가 없는데 keepUrlsOrder에 new:i 토큰이 포함되어 있습니다.");
					}
				} else {
					// 토큰 인덱스 집합은 정확히 {0..n-1}와 동일해야 함
					Set<Integer> expected = new LinkedHashSet<>();
					for (int i = 0; i < uploadedUrls.size(); i++) expected.add(i);
					if (!tokenIdx.equals(expected)) {
						return ResponseEntity.badRequest().body(
							"keepUrlsOrder의 new:i 토큰 수/인덱스가 업로드한 새 이미지 수와 일치하지 않습니다. " +
								"(expected: new:0..new:" + (uploadedUrls.size() - 1) + ")"
						);
					}
				}

				// 3-3) 최종 리스트 조립 ("new:i" → 업로드 URL 치환, 나머지는 기존 URL로 간주)
				for (String item : keepUrlsOrder) {
					if (item == null || item.isBlank()) continue;
					Matcher m = p.matcher(item);
					if (m.matches()) {
						int idx = Integer.parseInt(m.group(1));
						if (idx < 0 || idx >= uploadedUrls.size()) {
							return ResponseEntity.badRequest().body("잘못된 new 토큰 인덱스: " + item);
						}
						finalUrls.add(uploadedUrls.get(idx));
					} else {
						finalUrls.add(item); // 기존 URL
					}
				}
			}

			// 4) 최대 개수 제한
			if (finalUrls.size() > 5) {
				return ResponseEntity.badRequest().body("이미지는 최대 5장까지만 설정할 수 있습니다.");
			}

			// 5) 서비스 호출 (finalUrls가 곧 최종 상태/순서)
			spaceAdminService.updateWithImages(adminId, spaceId, request, finalUrls);

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
	public ResponseEntity<DeleteSpaceResponseDto> delete(Authentication authentication, @PathVariable Integer spaceId) {
		// 관리자 권한 아이디
		Long adminId = Long.valueOf((String) authentication.getPrincipal());

		spaceAdminService.deleteSpace(adminId, spaceId);
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
	public ResponseEntity<?> createTag(Authentication authentication, @RequestParam String tagName) {
		// 관리자 권한 아이디
		Long adminId = Long.valueOf((String) authentication.getPrincipal());

		// 입력 파라미터 유효성 검사
		if (tagName == null || tagName.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		try {
			SpaceTag createdTag = spaceAdminService.createTag(adminId, tagName);
			return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
		} catch (IllegalArgumentException e) {
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Conflict");
			errorResponse.put("message", "이미 존재하는 태그입니다.");

			return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
		}
	}

	/**
	 * 지역 아이디로 승인자 리스트 조회
	 **/
	@GetMapping("/approvers/{regionId}")
	@Parameter(name = "regionId", in = ParameterIn.PATH, description = "조회할 지역 ID", required = true)
	@Operation(summary = "지역 아이디로 승인자 리스트 조회", description = "토큰을 확인하여 지역 관리자 리스트(1차, 2차 승인자 포함)를 조회합니다.")
	public List<AdminListResponseDto> getApproversByRegionId(@PathVariable Integer regionId) {
		return spaceAdminService.getApproversByRegionId(regionId);
	}
}
