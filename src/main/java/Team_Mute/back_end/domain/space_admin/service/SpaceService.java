package Team_Mute.back_end.domain.space_admin.service;

import Team_Mute.back_end.domain.space_admin.dto.CategoryListItem;
import Team_Mute.back_end.domain.space_admin.dto.RegionListItem;
import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateRequest;
import Team_Mute.back_end.domain.space_admin.dto.SpaceListResponse;
import Team_Mute.back_end.domain.space_admin.dto.TagListItem;
import Team_Mute.back_end.domain.space_admin.entity.AdminRegion;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.entity.SpaceCategory;
import Team_Mute.back_end.domain.space_admin.entity.SpaceImage;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTag;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTagMap;
import Team_Mute.back_end.domain.space_admin.repository.AdminRegionRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceCategoryRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceImageRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceTagMapRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceTagRepository;
import Team_Mute.back_end.domain.space_admin.util.S3Deleter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpaceService {

	private final SpaceRepository spaceRepository;
	private final SpaceCategoryRepository categoryRepository;
	private final AdminRegionRepository regionRepository;
	private final SpaceTagRepository tagRepository;
	private final SpaceTagMapRepository tagMapRepository;
	private final SpaceImageRepository spaceImageRepository;
	private final S3Deleter s3Deleter;

	public SpaceService(
		SpaceRepository spaceRepository,
		SpaceCategoryRepository categoryRepository,
		AdminRegionRepository regionRepository,
		SpaceTagRepository tagRepository,
		SpaceTagMapRepository tagMapRepository,
		SpaceImageRepository spaceImageRepository,
		S3Deleter s3Deleter
	) {
		this.spaceRepository = spaceRepository;
		this.categoryRepository = categoryRepository;
		this.regionRepository = regionRepository;
		this.tagRepository = tagRepository;
		this.tagMapRepository = tagMapRepository;
		this.spaceImageRepository = spaceImageRepository;
		this.s3Deleter = s3Deleter;
	}

	// 지역 전체 조회(공간 등록 및 수정할 시 사용)
	public List<RegionListItem> getAllRegions() {
		return regionRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
			.stream()
			.map(element -> new RegionListItem(element.getId(), element.getRegionName()))
			.toList();
	}

	// 카테고리 전체 조회(공간 등록 및 수정할 시 사용)
	public List<CategoryListItem> getAllCategories() {
		return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
			.stream()
			.map(element -> new CategoryListItem(element.getId(), element.getCategoryName()))
			.toList();
	}

	// 태그 전체 조회(공간 등록 및 수정할 시 사용)
	public List<TagListItem> getAllTags() {
		return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
			.stream()
			.map(element -> new TagListItem(element.getId(), element.getTagName()))
			.toList();
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

		/* 대표(메인) 이미지 교체 시, 기존 S3 오브젝트 삭제*/
		// 기존 대표 이미지 URL
		String oldMainUrl = space.getSpaceImageUrl();
		String newMainUrl = (urls != null && !urls.isEmpty()) ? urls.get(0) : null;

		if (newMainUrl != null && oldMainUrl != null && !newMainUrl.equals(oldMainUrl)) {
			s3Deleter.deleteByUrl(oldMainUrl); // 기존 대표 이미지 삭제
		}

		// 기존 상세 이미지 URL 목록 DB에서 조회
		List<SpaceImage> oldImages = spaceImageRepository.findBySpace(space); // 직접 조회
		List<String> oldGalleryUrls = oldImages.stream()
			.map(SpaceImage::getImageUrl)
			.toList();

		// 새 상세 이미지 URL 목록 (urls의 1번 인덱스부터)
		List<String> newGalleryUrls = (urls != null && urls.size() > 1)
			? urls.subList(1, urls.size())
			: java.util.Collections.emptyList();

		// 기존에 있었는데 새 목록에 없는 것들 삭제
		for (String oldUrl : oldGalleryUrls) {
			if (!newGalleryUrls.contains(oldUrl)) {
				s3Deleter.deleteByUrl(oldUrl);
			}
		}

		if (urls != null) {
			// 상세 이미지 전부 삭제 (리포지토리에 메서드 필요)
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

	// 공간 삭제
	@Transactional
	public void deleteSpace(Integer spaceId) {
		// 1) 존재 확인
		Space space = spaceRepository.findById(spaceId)
			.orElseThrow(() -> new NoSuchElementException("Space not found: " + spaceId));

		// 2) S3 먼저 삭제 (실패 시 예외 → 트랜잭션 롤백)
		// 2-1) 대표(커버) 이미지
		String coverUrl = space.getSpaceImageUrl();
		if (coverUrl != null && !coverUrl.isBlank()) {
			s3Deleter.deleteByUrl(coverUrl);
		}

		// 2-2) 갤러리 이미지들
		if (space.getImages() != null) {
			for (SpaceImage img : space.getImages()) {
				String url = img.getImageUrl();
				if (url != null && !url.isBlank()) {
					s3Deleter.deleteByUrl(url);
				}
			}
		}

		// 3) DB 삭제 (연관 테이블은 CASCADE/ON DELETE CASCADE로 함께 정리)
		spaceRepository.delete(space);
	}


}
