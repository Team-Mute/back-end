package Team_Mute.back_end.domain.space_admin.controller;

import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.service.SpaceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spaces/admin")
public class SpaceController {

	private final SpaceService spaceService;

	public SpaceController(SpaceService spaceService) {
		this.spaceService = spaceService;
	}

	@GetMapping
	public List<Space> getSpaces() {
		return spaceService.getAllSpaces();
	}
}
