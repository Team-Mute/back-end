package Team_Mute.back_end.domain.space_user.service;


import Team_Mute.back_end.domain.space_user.dto.SpaceUserDtailResponseDto;
import Team_Mute.back_end.domain.space_user.dto.SpaceUserResponseDto;
import Team_Mute.back_end.domain.space_user.repository.SpaceUserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class SpaceUserService {
	private final SpaceUserRepository spaceUserRepository;

	public SpaceUserService(
		SpaceUserRepository spaceUserRepository
	) {
		this.spaceUserRepository = spaceUserRepository;
	}

	public List<SpaceUserResponseDto> searchSpaces(
		Integer regionId,          // CHANGE: null이면 전체
		Integer categoryId,        // CHANGE: null이면 전체
		Integer people,            // CHANGE: null 또는 <=0이면 전체
		String[] tagNames          // CHANGE: null/빈 배열이면 전체
	) {
		// CHANGE: people 보정 (0 이하 → 미적용)
		Integer safePeople = (people == null || people <= 0) ? null : people;

		// CHANGE: tagNames 보정 (null/빈 배열 → 전체 허용 분기)
		int tagCount = (tagNames == null) ? 0 : tagNames.length;
		String[] safeTags = (tagCount == 0) ? new String[]{} : tagNames;

		return spaceUserRepository.searchSpacesForUser(
			regionId, categoryId, safePeople, safeTags, tagCount
		);
	}

	// 특정 공간 상세 정보 조회
	public SpaceUserDtailResponseDto getSpaceById(Integer spaceId) {
		return spaceUserRepository.findSpaceDetail(spaceId)
			.orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
	}
}
