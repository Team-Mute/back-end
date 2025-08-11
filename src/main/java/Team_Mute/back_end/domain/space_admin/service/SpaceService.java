package Team_Mute.back_end.domain.space_admin.service;

import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateRequest;
import Team_Mute.back_end.domain.space_admin.dto.SpaceListResponse;
import Team_Mute.back_end.domain.space_admin.entity.*;
import Team_Mute.back_end.domain.space_admin.repository.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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

	// 공간 전체 조회
	public List<SpaceListResponse> getAllSpaces() {
		return spaceRepository.findAllWithNames();
	}

	// 특정 공간 조회
	public SpaceListResponse getSpaceById(Integer spaceId) {
		return spaceRepository.findDetailWithNames(spaceId)
			.orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
	}

	// 공간 등록
	@Transactional
	public Integer createWithImages(SpaceCreateRequest req, java.util.List<String> urls) {
		if (spaceRepository.existsBySpaceName(req.getSpaceName())) {
			throw new IllegalArgumentException("이미 존재하는 공간명입니다.");
		}

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
			.regDate(LocalDateTime.now())
			.build();

		Space saved = spaceRepository.save(space);

		// 4. 태그 처리
		for (String tagName : req.getTagNames()) {
			SpaceTag tag = tagRepository.findByTagName(tagName)
				.orElseGet(() -> {
					SpaceTag newTag = SpaceTag.builder()
						.tagName(tagName)
						.regDate(LocalDateTime.now())
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

	// 공간 수정
	@Transactional
	public void updateWithImages(Integer spaceId,
								 SpaceCreateRequest req,
								 java.util.List<String> urls) {

		// 1) 대상 조회
		Space space = spaceRepository.findById(spaceId)
			.orElseThrow(() -> new IllegalArgumentException("해당 공간이 존재하지 않습니다: " + spaceId));

		// 2) 이름이 요청에 포함되어 있고, 실제로 값이 바뀌는 경우만 중복 확인
		// 변경 감지 전 전처리(트리밍/정규화 권장)
		String newName = req.getSpaceName() != null ? req.getSpaceName().trim() : null;
		if (newName != null && !newName.equals(space.getSpaceName())) {
			if (spaceRepository.existsBySpaceNameAndSpaceIdNot(newName, spaceId)) {
				throw new DuplicateKeyException("이미 존재하는 공간명입니다.");
			}
			space.setSpaceName(newName);
		}

		// 3) categoryName → categoryId
		SpaceCategory category = categoryRepository.findByCategoryName(req.getCategoryName())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + req.getCategoryName()));

		// 4) regionName → regionId
		AdminRegion region = regionRepository.findByRegionName(req.getRegionName())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역명입니다: " + req.getRegionName()));

		// 5) 본문 필드 “전체 교체”
		space.setCategoryId(category.getId());
		space.setRegionId(region.getId());
		space.setUserId(req.getUserId());
		space.setSpaceName(req.getSpaceName());
		space.setSpaceLocation(req.getSpaceLocation());
		space.setSpaceDescription(req.getSpaceDescription());
		space.setSpaceCapacity(req.getSpaceCapacity());
		space.setSpaceIsAvailable(req.getSpaceIsAvailable());
        space.setUpdDate(LocalDateTime.now());

		// 6) 태그 전량 교체
		tagMapRepository.deleteBySpace(space);
		for (String tagName : req.getTagNames()) {
			SpaceTag tag = tagRepository.findByTagName(tagName)
				.orElseGet(() -> {
					SpaceTag newTag = SpaceTag.builder()
						.tagName(tagName)
						.regDate(LocalDateTime.now())
						.build();
					return tagRepository.save(newTag);
				});

			SpaceTagMap map = SpaceTagMap.builder()
				.space(space)
				.tag(tag)
				.regDate(LocalDateTime.now())
				.build();
			tagMapRepository.save(map);
		}

		// 7) 이미지 처리 (PUT 정책)
		//    - urls == null        : 이미지 변경 없음 (기존 커버/상세 유지)
		//    - urls.isEmpty()      : 커버/상세 전부 삭제 (커버 null, 상세 0장)
		//    - urls.size() >= 1    : 커버/상세 전부 교체
		if (urls != null) {
			// 상세 이미지 전부 삭제 (리포지토리에 메서드 필요)
			// SpaceImageRepository: void deleteBySpace(Space space);
			spaceImageRepository.deleteBySpace(space);

			if (urls.isEmpty()) {
				space.setSpaceImageUrl(null); // 커버 제거
			} else {
				// 커버 이미지 교체
				String cover = urls.get(0);
				space.setSpaceImageUrl(cover);

				// 상세 이미지 재등록 (우선순위 1..n)
				if (urls.size() > 1) {
					int p = 1;
					List<SpaceImage> list = new ArrayList<>(urls.size() - 1);
					for (String url : urls.subList(1, urls.size())) {
						SpaceImage si = new SpaceImage();
						si.setSpace(space);
						si.setImageUrl(url);
						si.setImagePriority(p++);
						list.add(si);
					}
					spaceImageRepository.saveAll(list);
				}
			}
		}
	}
}
