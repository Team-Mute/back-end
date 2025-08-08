package Team_Mute.back_end.domain.space_admin.service;

import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateRequest;
import Team_Mute.back_end.domain.space_admin.entity.*;
	import Team_Mute.back_end.domain.space_admin.repository.*;
	import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SpaceService {

	private final SpaceRepository spaceRepository;
	private final SpaceCategoryRepository categoryRepository;
	private final AdminRegionRepository regionRepository;
	private final SpaceTagRepository tagRepository;
	private final SpaceTagMapRepository tagMapRepository;

	public SpaceService(
		SpaceRepository spaceRepository,
		SpaceCategoryRepository categoryRepository,
		AdminRegionRepository regionRepository,
		SpaceTagRepository tagRepository,
		SpaceTagMapRepository tagMapRepository
	) {
		this.spaceRepository = spaceRepository;
		this.categoryRepository = categoryRepository;
		this.regionRepository = regionRepository;
		this.tagRepository = tagRepository;
		this.tagMapRepository = tagMapRepository;
	}

	public List<Space> getAllSpaces() {
		return spaceRepository.findAll();
	}

	@Transactional
	public Integer createSpace(SpaceCreateRequest req, String imageUrl) {

		// 1. categoryName → categoryId
		SpaceCategory category = categoryRepository.findByCategoryName(req.getCategoryName())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + req.getCategoryName()));

		// 2. regionName → regionId
		AdminRegion region = regionRepository.findByRegionName(req.getRegionName())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역명입니다: " + req.getRegionName()));

		// 3. 공간 저장
		Space space = Space.builder()
			.categoryId(category.getId())
			.regionId(region.getId())
			.userId(req.getUserId())
			.spaceName(req.getSpaceName())
			.spaceLocation(req.getSpaceLocation())
			.spaceDescription(req.getSpaceDescription())
			.spaceCapacity(req.getSpaceCapacity())
			.spaceAvailable(req.getSpaceAvailable())
			.regDate(LocalDateTime.now())
			.updDate(LocalDateTime.now())
			.build();

		space.setImageUrl(imageUrl);

		Space saved = spaceRepository.save(space);

		// 4. 태그 처리
		for (String tagName : req.getTagNames()) {
			SpaceTag tag = tagRepository.findByTagName(tagName)
				.orElseGet(() -> {
					SpaceTag newTag = SpaceTag.builder()
						.tagName(tagName)
						.regDate(LocalDateTime.now())
						.updDate(LocalDateTime.now())
						.build();
					return tagRepository.save(newTag);
				});

			SpaceTagMap map = SpaceTagMap.builder()
				.space(saved)
				.tag(tag)
				.regDate(LocalDateTime.now())
				.build();

			tagMapRepository.save(map);
		}

		return saved.getId();
	}
}

