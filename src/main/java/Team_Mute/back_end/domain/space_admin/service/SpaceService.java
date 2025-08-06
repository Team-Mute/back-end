package Team_Mute.back_end.domain.space_admin.service;

import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpaceService {

	private final SpaceRepository spaceRepository;

	public SpaceService(SpaceRepository spaceRepository) {
		this.spaceRepository = spaceRepository;
	}

	public List<Space> getAllSpaces() {
		return spaceRepository.findAll();
	}
}
