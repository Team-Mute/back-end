package Team_Mute.back_end.domain.space_admin.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.entity.AdminRegion;
import Team_Mute.back_end.domain.member.exception.UserNotFoundException;
import Team_Mute.back_end.domain.member.repository.AdminRegionRepository;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.space_admin.dto.request.SpaceCreateRequestDto;
import Team_Mute.back_end.domain.space_admin.dto.response.AdminListResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.SpaceDatailResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.SpaceListResponseDto;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.entity.SpaceCategory;
import Team_Mute.back_end.domain.space_admin.entity.SpaceClosedDay;
import Team_Mute.back_end.domain.space_admin.entity.SpaceImage;
import Team_Mute.back_end.domain.space_admin.entity.SpaceLocation;
import Team_Mute.back_end.domain.space_admin.entity.SpaceOperation;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTag;
import Team_Mute.back_end.domain.space_admin.entity.SpaceTagMap;
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
import lombok.extern.slf4j.Slf4j;

/**
 * 공간 관리 Service
 * - 관리자 전용 비즈니스 로직을 처리하는 계층
 * - Controller에서 전달받은 요청을 DB/Repository와 연동하여 처리
 * - 공간 등록, 수정, 삭제, 상세 조회 기능을 담당
 * - 트랜잭션 단위로 실행되어 원자성을 보장
 */
@Slf4j
@Service
public class SpaceAdminService {
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
	private final UserRepository userRepository;
	private final AdminRepository adminRepository;

	private static final Integer ROLE_MASTER = 0; // 마스터 관리자
	private static final Integer ROLE_SECOND_APPROVER = 1; // 2차 승인자
	private static final Integer ROLE_FIRST_APPROVER = 2; // 1차 승인자

	/**
	 * 공간 등록 및 수정 시, 이름으로 userId 결정
	 **/
	public Long resolveUserIdByUserName(String adminName) {
		String name = (adminName == null) ? "" : adminName.trim();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("담당자명이 비어 있습니다.");
		}

		List<Admin> matches = adminRepository.findByAdminName(name);
		if (matches.isEmpty()) {
			throw new IllegalArgumentException("존재하지 않는 담당자 이름입니다: " + name);
		}

		return matches.get(0).getAdminId();
	}

	public SpaceAdminService(
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
		SpaceLocationRepository spaceLocationRepository,
		UserRepository userRepository, AdminRepository adminRepository
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
		this.userRepository = userRepository;
		this.adminRepository = adminRepository;
	}

	/**
	 * 공간 전체 조회 (페이징 적용)
	 **/
	public Page<SpaceListResponseDto> getAllSpaces(Pageable pageable,
		Long adminId) { // This `Pageable` is the Spring one
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID

		// 1차 승인자일 경우, 담당 지역으로 필터링된 데이터 조회
		if (adminRole.equals(ROLE_FIRST_APPROVER) && admin.getAdminRegion() != null) {
			Integer adminRegionId = admin.getAdminRegion().getRegionId();
			return spaceRepository.findAllByAdminRegion(pageable, adminRegionId);
		}
		// 그 외 관리자(전체 조회 권한)일 경우, 모든 공간 데이터 조회
		else {
			return spaceRepository.findAllWithNames(pageable);
		}
	}

	/**
	 * 지역별 공간 전체 조회
	 **/
	public List<SpaceListResponseDto> getAllSpacesByRegion(Integer regionId) {
		return spaceRepository.findAllWithRegion(regionId);
	}

	/**
	 * 특정 공간 조회
	 **/
	public SpaceDatailResponseDto getSpaceById(Integer spaceId) {
		return spaceRepository.findDetailWithNames(spaceId)
			.orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
	}

	/**
	 * 공간 등록
	 **/
	@Transactional
	public Integer createWithImages(Long adminId, SpaceCreateRequestDto req, java.util.List<String> urls) {
		// 관리자 권한 체크
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID

		if (adminRole.equals(ROLE_MASTER)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공간 등록 권한이 없습니다.");
		}

		if (spaceRepository.existsBySpaceName(req.getSpaceName())) {
			throw new IllegalArgumentException("이미 존재하는 공간명입니다.");
		}

		// 1. categoryId
		SpaceCategory category = categoryRepository.findByCategoryId(req.getCategoryId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID입니다: " + req.getCategoryId()));

		// 2. regionId
		AdminRegion region = regionRepository.findByRegionId(req.getRegionId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역 ID입니다: " + req.getRegionId()));

		// 3. locationId
		SpaceLocation location = spaceLocationRepository.findByLocationId(req.getLocationId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주소 ID입니다: " + req.getLocationId()));

		// 4. 담당자 ID 및 지역 권한 검증
		Long adminIdToAssign = req.getAdminId();

		// Admin, UserRole, AdminRegion 정보를 함께 로드
		Admin assignedAdmin = adminRepository.findAdminWithRoleAndRegion(adminIdToAssign)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 담당자 ID입니다: " + adminIdToAssign));

		// 1차 승인자 (roleId=2) 권한 검증 / 2차 승인자 (roleId=1L)는 지역 검증을 건너뜀 (전역 권한)
		if (assignedAdmin.getUserRole().getRoleId().equals(2)) {
			Integer requiredRegionId = req.getRegionId();
			Integer adminRegionId = assignedAdmin.getAdminRegion() != null
				? assignedAdmin.getAdminRegion().getRegionId()
				: null;

			if (adminRegionId == null || !adminRegionId.equals(requiredRegionId)) {
				throw new IllegalArgumentException(
					"담당자 지정 불가 - 해당 지역에 대한 권한이 없습니다."
				);
			}
		}

		// 5. 공간 저장
		Space space = Space.builder()
			.categoryId(category.getCategoryId())
			.regionId(region.getRegionId())
			.userId(adminIdToAssign)
			.spaceName(req.getSpaceName())
			.locationId(location.getLocationId())
			.spaceDescription(req.getSpaceDescription())
			.spaceCapacity(req.getSpaceCapacity())
			.spaceIsAvailable(req.getSpaceIsAvailable())
			.reservationWay(req.getReservationWay())
			.spaceRules(req.getSpaceRules())
			.regDate(LocalDateTime.now())
			.build();

		Space saved = spaceRepository.save(space);
		Integer spaceId = saved.getSpaceId();

		// 6. 이미지 저장
		// 임시 폴더의 이미지들을 최종 폴더('spaces/{id}')로 이동
		String targetDir = "spaces/" + spaceId;
		List<String> finalUrls = urls.stream()
			.map(tempUrl -> {
				// S3Uploader의 copyByUrl 메서드를 사용하여 이미지 복사
				String finalUrl = s3Uploader.copyByUrl(tempUrl, targetDir);
				// 복사된 원본 임시 파일 삭제
				s3Deleter.deleteByUrl(tempUrl);
				return finalUrl;
			})
			.collect(Collectors.toList());

		// 최종 URL들을 사용하여 커버 및 상세 이미지를 DB에 저장
		String cover = finalUrls.isEmpty() ? null : finalUrls.get(0);
		List<String> details = finalUrls.size() > 1 ? finalUrls.subList(1, finalUrls.size()) : List.of();

		saved.setSpaceImageUrl(cover);

		// 상세 이미지 저장 (우선순위 1..n)
		if (!details.isEmpty()) {
			int p = 1;
			List<SpaceImage> list = new ArrayList<>(details.size());
			for (String url : details) {
				SpaceImage si = new SpaceImage();
				si.setSpace(saved);
				si.setImageUrl(url);
				si.setImagePriority(p++);
				list.add(si);
			}
			spaceImageRepository.saveAll(list);
		}

		// 7. 태그 처리
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

		// 8. 운영시간 저장
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

		// 9. 운영시간 및 휴무일 저장
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

	/**
	 * 공간 수정
	 **/
	@Transactional
	public void updateWithImages(Long adminId,
		Integer spaceId,
		SpaceCreateRequestDto req,
		java.util.List<String> urls) {
		// 관리자 권한 체크
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID

		if (adminRole.equals(ROLE_MASTER)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공간 수정 권한이 없습니다.");
		}

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

		// 7) 담당자 ID 및 지역 권한 검증
		Long adminIdToAssign = req.getAdminId();

		// Admin, UserRole, AdminRegion 정보를 함께 로드
		Admin assignedAdmin = adminRepository.findAdminWithRoleAndRegion(adminIdToAssign)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 담당자 ID입니다: " + adminIdToAssign));

		// 1차 승인자 (roleId=2) 권한 검증 / 2차 승인자 (roleId=1L)는 지역 검증을 건너뜀 (전역 권한)
		if (assignedAdmin.getUserRole().getRoleId().equals(2)) {
			Integer requiredRegionId = req.getRegionId();
			Integer adminRegionId = assignedAdmin.getAdminRegion() != null
				? assignedAdmin.getAdminRegion().getRegionId()
				: null;

			if (adminRegionId == null || !adminRegionId.equals(requiredRegionId)) {
				throw new IllegalArgumentException(
					"담당자 지정 불가 - 해당 지역에 대한 권한이 없습니다."
				);
			}
		}

		// 8) 본문 필드 “전체 교체”
		space.setCategoryId(category.getCategoryId());
		space.setRegionId(region.getRegionId());
		space.setUserId(adminIdToAssign);
		space.setSpaceName(req.getSpaceName());
		space.setLocationId(location.getLocationId());
		space.setSpaceDescription(req.getSpaceDescription());
		space.setSpaceCapacity(req.getSpaceCapacity());
		space.setSpaceIsAvailable(req.getSpaceIsAvailable());
		space.setUpdDate(LocalDateTime.now());
		space.setReservationWay(req.getReservationWay());
		space.setSpaceRules(req.getSpaceRules());

		// 9) 태그 전량 교체
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

		// 10) 운영 시간 및 휴무일 처리
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

		// 11) 이미지 처리 (PUT 정책)
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

	/**
	 * 공간 삭제
	 **/
	@Transactional
	public void deleteSpace(Long adminId, Integer spaceId) {
		// 관리자 권한 체크
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID

		if (adminRole.equals(ROLE_MASTER)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공간 삭제 권한이 없습니다.");
		}

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
	 * 태그(편의시설) 추가
	 **/
	public SpaceTag createTag(Long adminId, String tagName) {
		// 관리자 권한 체크
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID

		if (adminRole.equals(ROLE_MASTER)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리 권한이 없습니다.");
		}

		// 중복 태그 검증
		if (tagRepository.findByTagName(tagName).isPresent()) {
			throw new IllegalArgumentException("이미 존재하는 태그입니다.");
		}

		SpaceTag newTag = SpaceTag.builder()
			.tagName(tagName)
			.regDate(LocalDateTime.now())
			.build();

		return tagRepository.save(newTag);
	}

	/**
	 * 지역 ID로 승인자 리스트 조회 (role_id 1(2차 승인자) + role_id 2(1차 승인자) & regionId 일치)
	 **/
	public List<AdminListResponseDto> getApproversByRegionId(Integer regionId) {
		return adminRepository.findApproversByRegion(regionId)
			.stream()
			.map(admin -> {
				// roleId에 따른 역할 이름 결정
				String roleName = admin.getUserRole().getRoleId().equals(1) ? "2차 승인자" : "1차 승인자";

				// 출력 예시: 홍길동(1차 승인자) 형식으로 조합
				String adminNameWithRole = String.format("%s(%s)", admin.getAdminName(), roleName);

				// DTO로 변환 시 adminId도 함께 전달
				return new AdminListResponseDto(
					admin.getAdminId(),
					adminNameWithRole
				);
			})
			.toList();
	}
}
