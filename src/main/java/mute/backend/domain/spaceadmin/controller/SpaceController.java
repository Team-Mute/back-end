package mute.backend.domain.spaceadmin.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import mute.backend.domain.spaceadmin.dto.DeleteSpaceResponse;
import mute.backend.domain.spaceadmin.dto.SpaceCreateRequest;
import mute.backend.domain.spaceadmin.dto.SpaceListResponse;
import mute.backend.domain.spaceadmin.service.SpaceService;
import mute.backend.domain.spaceadmin.util.S3Uploader;
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
      @RequestPart("images") List<MultipartFile> images) {
    try {
      // 이미지가 없을 경우 예외 처리
      boolean noUsableFiles =
          (images == null || images.isEmpty())
              || images.stream().allMatch(f -> f == null || f.isEmpty());

      if (noUsableFiles) {
        return ResponseEntity.badRequest().body("이미지는 최소 1장은 필요합니다.");
      }

      List<String> urls = s3Uploader.uploadAll(images, "spaces"); // throws IOException 버전
      Integer id = spaceService.createWithImages(request, urls);

      return ResponseEntity.ok(spaceService.getSpaceById(id));

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
      @RequestPart(value = "images", required = false) List<MultipartFile> images) {
    try {
      // 이미지가 없을 경우 예외 처리
      boolean noUsableFiles =
          (images == null || images.isEmpty())
              || images.stream().allMatch(f -> f == null || f.isEmpty());

      if (noUsableFiles) {
        return ResponseEntity.badRequest().body("이미지는 최소 1장은 필요합니다.");
      }

      List<String> urls =
          (images != null && !images.isEmpty())
              ? s3Uploader.uploadAll(images, "spaces")
              : null; // 이미지 변경 없으면 null

      spaceService.updateWithImages(spaceId, request, urls);

      return ResponseEntity.ok(spaceService.getSpaceById(spaceId));
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
    return ResponseEntity.ok(new DeleteSpaceResponse("공간 삭제 완료", spaceId));
  }
}
