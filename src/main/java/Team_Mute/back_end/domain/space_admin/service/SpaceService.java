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
	private final SpaceImageRepository spaceImageRepository;

	public SpaceService(
		SpaceRepository spaceRepository,
		SpaceCategoryRepository categoryRepository,
		AdminRegionRepository regionRepository,
		SpaceTagRepository tagRepository,
		SpaceTagMapRepository tagMapRepository,
		SpaceImageRepository spaceImageRepository
	) {
		this.spaceRepository = spaceRepository;
		this.categoryRepository = categoryRepository;
		this.regionRepository = regionRepository;
		this.tagRepository = tagRepository;
		this.tagMapRepository = tagMapRepository;
		this.spaceImageRepository = spaceImageRepository;
	}

	public List<Space> getAllSpaces() {
		return spaceRepository.findAll();
	}

	@Transactional
	public Integer createWithImages(SpaceCreateRequest req, java.util.List<String> urls) {
		String cover = urls.get(0);
		java.util.List<String> details = urls.size() > 1 ? urls.subList(1, urls.size()) : java.util.List.of();

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
			.spaceIsAvailable(req.getSpaceIsAvailable())
			.spaceImageUrl(cover)
			.build();

		Space saved = spaceRepository.save(space);

		// 2) 상세 이미지 저장 (우선순위 1..n)
		if (!details.isEmpty()) {
			int p = 1;
			java.util.List<SpaceImage> list = new java.util.ArrayList<>(details.size());
			for (String url : details) {
				SpaceImage si = new SpaceImage();
				si.setSpace(saved);      // FK 연결 (ManyToOne 사용 중일 때)
				si.setImageUrl(url);
				si.setImagePriority(p++);
				list.add(si);
			}
			spaceImageRepository.saveAll(list);
		}

		// PK getter 이름 맞추기
		return saved.getSpaceId();
	}
}
