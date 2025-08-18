package Team_Mute.back_end.domain.space_admin.service;

import Team_Mute.back_end.domain.space_admin.dto.CategoryListItem;
import Team_Mute.back_end.domain.space_admin.dto.LocationListItem;
import Team_Mute.back_end.domain.space_admin.dto.RegionListItem;
import Team_Mute.back_end.domain.space_admin.dto.SpaceCreateRequest;
import Team_Mute.back_end.domain.space_admin.dto.SpaceListResponse;
import Team_Mute.back_end.domain.space_admin.dto.TagListItem;
import Team_Mute.back_end.domain.space_admin.entity.AdminRegion;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.entity.SpaceCategory;
import Team_Mute.back_end.domain.space_admin.entity.SpaceClosedDay;
import Team_Mute.back_end.domain.space_admin.entity.SpaceImage;
import Team_Mute.back_end.domain.space_admin.entity.SpaceLocation;
import Team_Mute.back_end.domain.space_admin.entity.SpaceOperation;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTag;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTagMap;
import Team_Mute.back_end.domain.space_admin.repository.AdminRegionRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceCategoryRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceClosedDayRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceImageRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceLocationRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceOperationRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceTagMapRepository;
import Team_Mute.back_end.domain.space_admin.repository.SpaceTagRepository;
import Team_Mute.back_end.domain.space_admin.util.S3Deleter;
import Team_Mute.back_end.domain.space_admin.util.S3Uploader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.BeanUtils;
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
	private final S3Uploader s3Uploader;
	private final S3Deleter s3Deleter;
	private final SpaceOperationRepository spaceOperationRepository;
	private final SpaceClosedDayRepository spaceClosedDayRepository;
	private final SpaceLocationRepository spaceLocationRepository;

	public SpaceService(
		SpaceRepository spaceRepository,
		SpaceCategoryRepository categoryRepository,
		AdminRegionRepository regionRepository,
		SpaceTagRepository tagRepository,
		SpaceTagMapRepository tagMapRepository,
		SpaceImageRepository spaceImageRepository,
		S3Uploader s3Uploader,
		S3Deleter s3Deleter,
		SpaceOperationRepository spaceOperationRepository,
		SpaceClosedDayRepository spaceClosedDayRepository,
		SpaceLocationRepository spaceLocationRepository
	) {
		this.spaceRepository = spaceRepository;
		this.categoryRepository = categoryRepository;
		this.regionRepository = regionRepository;
		this.tagRepository = tagRepository;
		this.tagMapRepository = tagMapRepository;
		this.spaceImageRepository = spaceImageRepository;
		this.s3Uploader = s3Uploader;
		this.s3Deleter = s3Deleter;
		this.spaceOperationRepository = spaceOperationRepository;
		this.spaceClosedDayRepository = spaceClosedDayRepository;
		this.spaceLocationRepository = spaceLocationRepository;
	}

	// 지역 전체 조회(공간 등록 및 수정할 시 사용)
	public List<RegionListItem> getAllRegions() {
		return regionRepository.findAll(Sort.by(Sort.Direction.ASC, "regionId"))
			.stream()
			.map(element -> new RegionListItem(element.getRegionId(), element.getRegionName()))
			.toList();
	}

	// 카테고리 전체 조회(공간 등록 및 수정할 시 사용)
	public List<CategoryListItem> getAllCategories() {
		return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryId"))
			.stream()
			.map(element -> new CategoryListItem(element.getCategoryId(), element.getCategoryName()))
			.toList();
	}

	// 태그 전체 조회(공간 등록 및 수정할 시 사용)
	public List<TagListItem> getAllTags() {
		return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "tagId"))
			.stream()
			.map(element -> new TagListItem(element.getTagId(), element.getTagName()))
			.toList();
	}

	// 지역 아이디로 주소 조회(공간 등록 및 수정할 시 사용)
	public List<LocationListItem> getLocationByRegionId(Integer regionId) {
		return spaceLocationRepository.findByRegionIdAndIsActiveTrueOrderByLocationNameAsc(regionId).stream()
			.map(element -> new LocationListItem(
				element.getLocationId(),
				element.getLocationName(),
				element.getAddressRoad(),
				element.getPostalCode()
			)).toList();
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

		// 1. categoryId
		SpaceCategory category = categoryRepository.findByCategoryId(req.getCategoryId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID입니다: " + req.getCategoryId()));

		// 2. regionId
		AdminRegion region = regionRepository.findByRegionId(req.getRegionId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역 ID입니다: " + req.getRegionId()));

		// 3. locationId
		SpaceLocation location = spaceLocationRepository.findByLocationId(req.getLocationId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주소 ID입니다: " + req.getLocationId()));

		// 4. 공간 저장
		Space space = Space.builder()
			.categoryId(category.getCategoryId())
			.regionId(region.getRegionId())
			.userId(req.getUserId())
			.spaceName(req.getSpaceName())
			.locationId(location.getLocationId())
			.spaceDescription(req.getSpaceDescription())
			.spaceCapacity(req.getSpaceCapacity())
			.spaceIsAvailable(req.getSpaceIsAvailable())
			.spaceImageUrl(cover)
			.reservationWay(req.getReservationWay())
			.spaceRules(req.getSpaceRules())
			.saveStatus(req.getSaveStatus())
			.regDate(LocalDateTime.now())
			.build();

		Space saved = spaceRepository.save(space);

		// 5. 태그 처리
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

		// 상세 이미지 저장 (우선순위 1..n)
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

		// 운영시간 저장
		if (req.getOperations() != null && !req.getOperations().isEmpty()) {
			List<SpaceOperation> ops = req.getOperations().stream().map(o ->
				SpaceOperation.builder()
					.space(space)
					.day(o.getDay()) // day: 1=월 ~ 7=일
					.operationFrom(o.getFrom())
					.operationTo(o.getTo())
					.isOpen(Boolean.TRUE.equals(o.getIsOpen()))
					.build()
			).toList();
			spaceOperationRepository.saveAll(ops);
		}

		// 운영시간 및 휴무일 저장
		if (req.getClosedDays() != null && !req.getClosedDays().isEmpty()) {
			DateTimeFormatter f = DateTimeFormatter.ISO_DATE_TIME; // "2025-09-15T09:00:00"
			List<SpaceClosedDay> closedDay = req.getClosedDays().stream().map(c ->
				SpaceClosedDay.builder()
					.space(space)
					.closedFrom(c.getFrom())
					.closedTo(c.getTo())
					.build()
			).toList();
			spaceClosedDayRepository.saveAll(closedDay);
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

		// 3) categoryId
		SpaceCategory category = categoryRepository.findByCategoryId(req.getCategoryId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + req.getCategoryId()));

		// 4) regionId
		AdminRegion region = regionRepository.findByRegionId(req.getRegionId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역명입니다: " + req.getRegionId()));

		// 5) locationId
		SpaceLocation location = spaceLocationRepository.findByLocationId(req.getLocationId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주소 ID입니다: " + req.getLocationId()));

		// 6) 이미지 처리 직전, 최종 결과 기준 검증 로직
		if (urls == null) {
			// 변경 없음 → 기존 상태 유지
			boolean hasAny = (space.getSpaceImageUrl() != null)
				|| !spaceImageRepository.findBySpace(space).isEmpty();
			if (!hasAny) {
				throw new IllegalArgumentException("이미지는 최소 1장은 필요합니다.");
			}
		} else {
			// urls 기반 최종 결과 계산
			String newMainUrl = urls.isEmpty() ? null : urls.get(0);
			List<String> newGalleryUrls = (urls.size() > 1) ? urls.subList(1, urls.size()) : List.of();

			boolean resultEmpty = (newMainUrl == null) && newGalleryUrls.isEmpty();
			if (resultEmpty) {
				throw new IllegalArgumentException("이미지는 최소 1장은 필요합니다.");
			}
		}


		// 7) 본문 필드 “전체 교체”
		space.setCategoryId(category.getCategoryId());
		space.setRegionId(region.getRegionId());
		space.setUserId(req.getUserId());
		space.setSpaceName(req.getSpaceName());
		space.setLocationId(location.getLocationId());
		space.setSpaceDescription(req.getSpaceDescription());
		space.setSpaceCapacity(req.getSpaceCapacity());
		space.setSpaceIsAvailable(req.getSpaceIsAvailable());
		space.setUpdDate(LocalDateTime.now());
		space.setReservationWay(req.getReservationWay());
		space.setSpaceRules(req.getSpaceRules());
		space.setSaveStatus(req.getSaveStatus());

		// 8) 태그 전량 교체
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

		// 9) 운영 시간 및 휴무일 처리
		// 운영시간
		spaceOperationRepository.deleteBySpaceId(spaceId);
		if (!req.getOperations().isEmpty()) {
			List<SpaceOperation> ops = req.getOperations().stream().map(o ->
				SpaceOperation.builder()
					.space(space)
					.day(o.getDay())
					.operationFrom(o.getFrom())
					.operationTo(o.getTo())
					.isOpen(Boolean.TRUE.equals(o.getIsOpen()))
					.build()
			).toList();
			spaceOperationRepository.saveAll(ops);
		}

		// 휴무일
		spaceClosedDayRepository.deleteBySpaceId(spaceId);
		if (!req.getClosedDays().isEmpty()) {
			DateTimeFormatter f = DateTimeFormatter.ISO_DATE_TIME;
			List<SpaceClosedDay> closedDay = req.getClosedDays().stream().map(c ->
				SpaceClosedDay.builder()
					.space(space)
					.closedFrom(c.getFrom())
					.closedTo(c.getTo())
					.build()
			).toList();
			spaceClosedDayRepository.saveAll(closedDay);
		}

		// 10) 이미지 처리 (PUT 정책)
		// - urls == null   : 이미지 변경 없음 (기존 유지, S3 조작 없음)
		// - urls.isEmpty() : 커버/상세 전부 삭제 (커버 null, 상세 0장) + S3 삭제
		// - urls.size()>=1 : 커버/상세 전부 교체 + S3 삭제(제거분만)

		if (urls != null) {
			// 기존 상태 스냅샷
			final String oldMainUrl = space.getSpaceImageUrl();
			final java.util.List<SpaceImage> oldImages = spaceImageRepository.findBySpace(space);
			final java.util.List<String> oldGalleryUrls = oldImages.stream()
				.map(SpaceImage::getImageUrl)
				.filter(java.util.Objects::nonNull)
				.collect(java.util.stream.Collectors.toList());

			// 새 상태 스냅샷
			final String newMainUrl = urls.isEmpty() ? null : urls.get(0);
			final java.util.List<String> newGalleryUrls = (urls.size() > 1)
				? urls.subList(1, urls.size())
				: java.util.Collections.emptyList();

			// --- 삭제 대상 URL 계산(메인 + 상세) ---
			java.util.List<String> deleteUrls = new java.util.ArrayList<>();

			// 메인 이미지: 전체 삭제 또는 교체면 삭제 대상
			if (oldMainUrl != null) {
				if (newMainUrl == null || !newMainUrl.equals(oldMainUrl)) {
					deleteUrls.add(oldMainUrl);
				}
			}

			// 상세 이미지: 기존 - 신규 차집합만 삭제
			for (String oldUrl : oldGalleryUrls) {
				if (!newGalleryUrls.contains(oldUrl)) {
					deleteUrls.add(oldUrl);
				}
			}

			// --- DB 갱신 ---
			// 상세 전량 삭제 후 재삽입(단순 PUT 정책)
			spaceImageRepository.deleteBySpace(space);

			if (urls.isEmpty()) {
				// 전체 삭제
				space.setSpaceImageUrl(null);
			} else {
				// 커버 교체
				space.setSpaceImageUrl(newMainUrl);

				// 상세 재등록 (우선순위 1..n)
				if (!newGalleryUrls.isEmpty()) {
					int p = 1;
					java.util.List<SpaceImage> list = new java.util.ArrayList<>(newGalleryUrls.size());
					for (String url : newGalleryUrls) {
						SpaceImage si = new SpaceImage();
						si.setSpace(space);
						si.setImageUrl(url);
						si.setImagePriority(p++);
						list.add(si);
					}
					spaceImageRepository.saveAll(list);
				}
			}

			// 변경사항 확정 (지연 flush 방지)
			spaceRepository.save(space);
			spaceRepository.flush();

			// --- 커밋 이후 S3 삭제 (DB 커밋 성공 시에만) ---
			if (!deleteUrls.isEmpty()) {
				org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
					new org.springframework.transaction.support.TransactionSynchronization() {
						@Override
						public void afterCommit() {
							for (String url : deleteUrls) {
								try {
									s3Deleter.deleteByUrl(url);
								} catch (Exception ignored) {
								}
							}
						}
					}
				);
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

	/**
	 * 바디 없이 spaceId만 받아 복제.
	 * - 이름: 원본 이름 + " (복제)", 중복 시 (복제 2), (복제 3)...
	 * - 상태: 항상 DRAFT
	 * - 이미지: S3 실제 복사 후 새 URL 저장
	 * - 태그/운영시간/휴무일: 깊은 복제
	 * - 반환: 상세 응답(프로젝트의 Projection/DTO)으로 반환
	 */
	@Transactional
	public SpaceListResponse cloneSpace(Integer sourceSpaceId) {
		// 1) 원본 조회
		Space src = spaceRepository.findById(sourceSpaceId)
			.orElseThrow(() -> new NoSuchElementException("원본 공간을 찾을 수 없습니다. spaceId=" + sourceSpaceId));

		// 2) 본문 복제 (스칼라 필드만 복사, 식별자/연관/감사 컬럼 제외)
		Space clone = new Space();
		BeanUtils.copyProperties(
			src, clone,
			"spaceId", "images", "tagMaps", "operations", "closedDays",
			"regDate", "updDate"
		);

		// 이름 중복 방지: "원본명 (복제)", 충돌 시 "원본명 (복제 2)", ...
		String baseName = (src.getSpaceName() != null && !src.getSpaceName().isBlank())
			? src.getSpaceName() : "새 공간";
		clone.setSpaceName(nextUniqueClonedName(baseName));

		clone.setSaveStatus("DRAFT"); // 저장 상태는 항상 DRAFT 강제
		clone.setRegDate(LocalDateTime.now()); // regDate = 복제 일시

		// 메인 이미지도 S3에 실제 복사 후 새 URL로 교체
		String mainUrl = src.getSpaceImageUrl();
		if (mainUrl != null && !mainUrl.isBlank()) {
			String copiedMainUrl = s3Uploader.copyByUrl(mainUrl, "spaces"); // 같은 디렉토리 규칙
			clone.setSpaceImageUrl(copiedMainUrl);
		}

		// 3) 저장 (식별자 확보)
		clone = spaceRepository.save(clone);
		final Integer clonedId = clone.getSpaceId(); // 람다에서 사용 위해 미리 캡쳐

		// 4) 이미지 복제 (S3 실복사)
		List<SpaceImage> srcImages = spaceImageRepository.findBySpace(src);
		if (srcImages != null && !srcImages.isEmpty()) {
			for (SpaceImage si : srcImages) {
				SpaceImage ni = new SpaceImage();
				ni.setSpace(clone);
				ni.setImagePriority(si.getImagePriority());

				String newUrl = null;
				if (si.getImageUrl() != null && !si.getImageUrl().isBlank()) {
					// 버킷 내 복사 → 새 키/URL
					newUrl = s3Uploader.copyByUrl(si.getImageUrl(), "spaces");
				}
				ni.setImageUrl(newUrl);
				spaceImageRepository.save(ni);
			}
		}

		// 5) 태그 매핑 복제
		if (src.getTagMaps() != null && !src.getTagMaps().isEmpty()) {
			for (SpaceTagMap map : src.getTagMaps()) {
				SpaceTagMap newMap = new SpaceTagMap();
				newMap.setSpace(clone);
				newMap.setTag(map.getTag()); // 동일 태그 참조
				tagMapRepository.save(newMap);
			}
		}

		// 6) 운영시간 복제
		List<SpaceOperation> ops = spaceOperationRepository.findAllBySpaceId(src.getSpaceId());
		if (ops != null && !ops.isEmpty()) {
			for (SpaceOperation o : ops) {
				SpaceOperation no = new SpaceOperation();
				no.setSpace(clone);
				no.setDay(o.getDay());
				no.setOperationFrom(o.getOperationFrom());
				no.setOperationTo(o.getOperationTo());
				no.setIsOpen(o.getIsOpen());
				spaceOperationRepository.save(no);
			}
		}

		// 7) 휴무일 복제
		List<SpaceClosedDay> cds = spaceClosedDayRepository.findAllBySpaceId(src.getSpaceId());
		if (cds != null && !cds.isEmpty()) {
			for (SpaceClosedDay c : cds) {
				SpaceClosedDay nc = new SpaceClosedDay();
				nc.setSpace(clone);
				nc.setClosedFrom(c.getClosedFrom());
				nc.setClosedTo(c.getClosedTo());
				spaceClosedDayRepository.save(nc);
			}
		}

		// 8) 복제 결과 상세 반환 (프로젝트의 기존 Projection/DTO 사용)
		return spaceRepository.findDetailWithNames(clonedId)
			.orElseThrow(() -> new IllegalStateException("복제된 공간 조회 실패: spaceId=" + clonedId));
	}

	/**
	 * "원본명 (복제)"가 존재하면 "원본명 (복제 2)", "원본명 (복제 3)" ... 로 유니크 이름 생성
	 * SpaceRepository에 existsBySpaceName(String name) 존재한다고 가정.
	 */
	private String nextUniqueClonedName(String baseName) {
		String suffix = " (복제)";
		String candidate = baseName + suffix;
		if (!spaceRepository.existsBySpaceName(candidate)) {
			return candidate;
		}
		int n = 2;
		while (true) {
			candidate = baseName + " (복제 " + n + ")";
			if (!spaceRepository.existsBySpaceName(candidate)) {
				return candidate;
			}
			n++;
		}
	}
}
