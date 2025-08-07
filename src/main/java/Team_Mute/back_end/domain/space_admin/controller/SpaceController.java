package Team_Mute.back_end.domain.space_admin.controller;

import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateRequest;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.service.SpaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spaces/admin")
public class SpaceController {

	private final SpaceService spaceService;

	public SpaceController(SpaceService spaceService) {
		this.spaceService = spaceService;
	}

	// 공간 조회
	@GetMapping
	public List<Space> getSpaces() {
		return spaceService.getAllSpaces();
	}

	// 공간 등록
	@PostMapping
	public ResponseEntity<?> createSpace(@RequestBody SpaceCreateRequest request) {
		try {
			Integer newSpaceId = spaceService.createSpace(request);
			return ResponseEntity.status(201).body("공간이 등록되었습니다. ID: " + newSpaceId);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("등록 실패: " + e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
		}
	}
}
