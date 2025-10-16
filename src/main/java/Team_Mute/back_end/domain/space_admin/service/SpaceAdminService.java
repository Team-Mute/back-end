package Team_Mute.back_end.domain.space_admin.service;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.entity.AdminRegion;
import Team_Mute.back_end.domain.member.exception.UserNotFoundException;
import Team_Mute.back_end.domain.member.repository.AdminRegionRepository;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
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
import Team_Mute.back_end.global.constants.AdminRoleEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

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
	// Repository 및 외부 유틸리티 의존성 주입
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
	private final AdminRepository adminRepository;
	private final EntityManager entityManager;

	// Constructor Injection (생성자를 통한 의존성 주입)
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
		AdminRepository adminRepository,
		EntityManager entityManager
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
		this.adminRepository = adminRepository;
		this.entityManager = entityManager;
	}

	/**
	 * 공간 전체 조회 (페이징 적용)
	 * - 1차 승인자({@code ROLE_FIRST_APPROVER})일 경우, 자신이 담당하는 지역의 공간만 조회
	 * - 2차 승인자 또는 다른 권한({@code ROLE_SECOND_APPROVER})일 경우, 모든 공간을 조회
	 *
	 * @param pageable Spring Data JPA의 페이징 정보 (페이지 번호, 크기, 정렬 등)
	 * @param adminId  현재 로그인한 관리자의 ID
	 * @return 페이징 처리된 {@code SpaceListResponseDto} 목록
	 * @throws UserNotFoundException 관리자 ID에 해당하는 사용자가 없을 경우
	 **/
	public Page<SpaceListResponseDto> getAllSpaces(Pageable pageable, Long adminId) { // This `Pageable` is the Spring one
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID

		// 1차 승인자일 경우, 담당 지역으로 필터링된 데이터 조회
		if (adminRole.equals(AdminRoleEnum.ROLE_FIRST_APPROVER.getId()) && admin.getAdminRegion() != null) {
			Integer adminRegionId = admin.getAdminRegion().getRegionId();
			return spaceRepository.findAllByAdminRegion(pageable, adminRegionId);
		}
		// 그 외 관리자(전체 조회 권한)일 경우, 모든 공간 데이터 조회
		else {
			return spaceRepository.findAllWithNames(pageable);
		}
	}

	/**
	 * 지역별 공간 전체 조회 (페이징 적용)
	 *
	 * @param pageable Spring Data JPA의 페이징 정보
	 * @param regionId 조회할 지역의 ID
	 * @return 페이징 처리된 {@code SpaceListResponseDto} 목록
	 **/
	public Page<SpaceListResponseDto> getAllSpacesByRegion(Pageable pageable, Integer regionId) {
		return spaceRepository.findAllWithRegion(pageable, regionId);
	}

	/**
	 * 특정 공간 조회
	 *
	 * @param spaceId 조회할 공간의 ID
	 * @return 공간 상세 정보 DTO (담당자 이름, 지역 이름 등을 포함)
	 * @throws NoSuchElementException 공간 ID에 해당하는 데이터가 없을 경우
	 **/
	@Transactional(readOnly = true)
	public SpaceDatailResponseDto getSpaceById(Integer spaceId, Long adminId) {
		// 관리자 권한 체크
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId();

		// 공간 존재 유무를 먼저 확인하고, 권한 체크에 사용할 엔티티를 가져옴
		// EntityManager를 사용하여 읽기 전용 힌트(LockModeType.NONE)를 명시적으로 적용
		Space space = entityManager.find(Space.class, spaceId, java.util.Map.of("jakarta.persistence.lock.timeout", LockModeType.NONE));

		// 1차 승인자일 경우, 담당 지역 확인 로직을 수행
		if (adminRole.equals(AdminRoleEnum.ROLE_FIRST_APPROVER.getId())) {
			Integer adminRegionId = admin.getAdminRegion().getRegionId(); // 로그인된 1차 승인자의 지역 아이디
			Integer spaceRegionId = space.getRegionId(); // 조회할 공간의 지역 아이디

			// 1차 승인자는 담당 지역이 아닐 경우 권한 없음 (403 FORBIDDEN)
			if (!adminRegionId.equals(spaceRegionId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 공간 접근 권한이 없습니다.");
			}
			// 권한 OK
		}
		// 그 외 관리자(전체 조회 권한)일 경우, if 블록을 건너뛰고 자동으로 권한 OK

		// 최종 상세 정보 조회 (공간 존재와 권한이 모두 확인된 후 실행)
		// findDetailWithNames는 DTO를 반환하므로, 혹시 모를 데이터 무결성 문제에 대비해 Optional 처리는 유지
		return spaceRepository.findDetailWithNames(spaceId)
			.orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
	}

	/**
	 * 공간 등록
	 * - 복잡한 다중 DB/S3 작업을 원자적으로 처리하기 위해 {@code @Transactional}을 사용
	 *
	 * @param adminId 공간 등록을 요청한 관리자 ID
	 * @param req     공간 생성 요청 DTO
	 * @param urls    S3 'temp' 폴더에 임시로 업로드된 이미지 URL 목록
	 * @return 새로 생성된 공간의 ID
	 * @throws ResponseStatusException  권한이 없는 경우 (403 FORBIDDEN)
	 * @throws IllegalArgumentException 공간명 중복, 카테고리/지역/주소/담당자 ID가 유효하지 않은 경우
	 **/
	@Transactional
	public Integer createWithImages(Long adminId, SpaceCreateRequestDto req, java.util.List<String> urls) {
		// 관리자 권한 체크
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId();

		// 마스터 권한({@code ROLE_MASTER, role_id = 0})은 공간 등록 권한이 없음
		if (adminRole.equals(AdminRoleEnum.ROLE_MASTER.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공간 등록 권한이 없습니다.");
		}
		// 1차 승인자({@code ROLE_FIRST_APPROVER,role_id = 2})은 담당 지역이 아닐 경우 공간 등록 권한이 없음
		if (adminRole.equals(AdminRoleEnum.ROLE_FIRST_APPROVER.getId()) && !admin.getAdminRegion().getRegionId().equals(req.getRegionId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 지역의 공간 등록 권한이 없습니다.");
		}

		// 공간명 중복 체크
		if (spaceRepository.existsBySpaceName(req.getSpaceName())) {
			throw new IllegalArgumentException("이미 존재하는 공간명입니다.");
		}

		// 필수 외래키(Foreign Key)들 엔티티 조회 및 유효성 검증
		// 1) categoryId
		SpaceCategory category = categoryRepository.findByCategoryId(req.getCategoryId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID입니다: " + req.getCategoryId()));

		// 2) regionId
		AdminRegion region = regionRepository.findByRegionId(req.getRegionId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역 ID입니다: " + req.getRegionId()));

		// 3) locationId
		SpaceLocation location = spaceLocationRepository.findByLocationId(req.getLocationId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주소 ID입니다: " + req.getLocationId()));

		// 담당자 ID 및 지역 권한 검증
		Long adminIdToAssign = req.getAdminId();

		// 담당자로 지정될 Admin, UserRole, AdminRegion 정보를 함께 로드
		Admin assignedAdmin = adminRepository.findAdminWithRoleAndRegion(adminIdToAssign)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 담당자 ID입니다: " + adminIdToAssign));

		// 1차 승인자 (roleId=2): 권한 검증(해당 담당자의 담당 지역과 일치하는지 검증)
		// 2차 승인자 (roleId=1): 지역 검증을 건너뜀 (전역 권한)
		if (assignedAdmin.getUserRole().getRoleId().equals(AdminRoleEnum.ROLE_FIRST_APPROVER.getId())) {
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

		// === 공간 정보 DB 저장 ===
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

		// === 이미지 저장 ===
		// 임시 폴더의 이미지들을 최종 폴더('spaces/{id}')로 이동
		String targetDir = "spaces/" + spaceId;
		// S3 롤백 처리용 리스트 선언
		java.util.List<String> successfullyCopiedUrls = new java.util.ArrayList<>();

		// 롤백 시 S3에 복사된 최종 파일들을 정리하기 위해 동기화 등록
		TransactionSynchronizationManager.registerSynchronization(
			new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
				@Override
				public void afterCompletion(int status) {
					// 트랜잭션이 롤백(STATUS_ROLLED_BACK)되었을 때만 처리
					if (status == org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK) {
						for (String url : successfullyCopiedUrls) {
							try {
								// 롤백 시 최종 폴더에 남아있는 파일을 삭제
								s3Deleter.deleteByUrl(url);
							} catch (Exception ignored) {
								// S3 롤백 삭제 실패 시 무시하거나 로깅 처리
							}
						}
					}
				}
			}
		);

		// 임시 폴더(temp)에 있는 이미지들을 최종 공간 폴더('spaces/{id}')로 복사(Copy)한 후, 원본 임시 파일은 즉시 삭제
		// 복사된 최종 파일의 URL은 'successfullyCopiedUrls' 리스트에 등록하여, 이후 DB 작업 중 예외 발생(트랜잭션 롤백) 시 S3에 잔여 파일이 남지 않도록 정리(Cleanup)를 예약
		List<String> finalUrls = urls.stream()
			.map(tempUrl -> {
				// S3Uploader의 copyByUrl 메서드를 사용하여 이미지 복사
				String finalUrl = s3Uploader.copyByUrl(tempUrl, targetDir);

				// 복사 성공 시, 롤백 시 삭제할 리스트에 추가
				successfullyCopiedUrls.add(finalUrl);

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

		// === 태그 처리 ===
		for (String tagName : req.getTagNames()) {
			// 태그명으로 조회 -> 없으면 예외 발생
			SpaceTag tag = tagRepository.findByTagName(tagName)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다: " + tagName));

			// Space와 Tag의 매핑 엔티티 생성 및 저장
			SpaceTagMap map = SpaceTagMap.builder()
				.space(saved)
				.tag(tag)
				.regDate(LocalDateTime.now())
				.build();

			tagMapRepository.save(map);
		}

		// === 운영시간 저장 ===
		if (req.getOperations() != null && !req.getOperations().isEmpty()) {

			// 1) 유효성 검사: operationFrom이 operationTo보다 이후이거나 같은지 확인
			boolean isValidTimeRange = req.getOperations().stream()
				.allMatch(o -> {
					// 운영 시간이 열려 있는 경우에만 시간 유효성을 검사합니다.
					if (Boolean.TRUE.equals(o.getIsOpen())) {
						// 시작 시간이 종료 시간보다 같거나 이후인 경우 (유효하지 않음)
						if (!o.getFrom().isBefore(o.getTo())) {
							return false;
						}
					}
					return true;
				});

			if (!isValidTimeRange) {
				// 유효하지 않은 시간 범위 예외 발생
				throw new IllegalArgumentException("운영 시간 '시작 시간(from)'은 '종료 시간(to)'보다 이후이거나 같을 수 없습니다.");
			}

			// 2) 유효성 검사 통과 후 DB 저장
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

		// === 휴무일 저장 ===
		if (req.getClosedDays() != null && !req.getClosedDays().isEmpty()) {

			// 1) 유효성 검사: closedFrom 날짜가 closedTo 날짜보다 이전인지 확인
			boolean isValidDateRange = req.getClosedDays().stream()
				.allMatch(c -> {
					// from 날짜가 to 날짜보다 이후인 경우 (유효하지 않음)
					if (c.getFrom().isAfter(c.getTo())) {
						return false;
					}
					return true;
				});

			if (!isValidDateRange) {
				// 유효하지 않은 날짜 범위 예외 발생
				throw new IllegalArgumentException("휴무일 '시작일(from)'은 '종료일(to)'보다 이후일 수 없습니다.");
			}

			// 2) 유효성 검사 통과 후 DB 저장
			List<SpaceClosedDay> closedDay = req.getClosedDays().stream().map(c ->
				SpaceClosedDay.builder()
					.space(space)
					.closedFrom(c.getFrom())
					.closedTo(c.getTo())
					.build()
			).toList();
			spaceClosedDayRepository.saveAll(closedDay);
		}

		// 새로 생성된 공간의 ID 반환
		return saved.getSpaceId();
	}

	/**
	 * 공간 수정
	 * - 이미지 처리는 PUT 정책(전체 교체)을 따르며, S3 삭제는 트랜잭션 커밋 후에 비동기적으로(afterCommit) 처리
	 *
	 * @param adminId       공간 수정을 요청한 관리자 ID
	 * @param spaceId       수정할 공간 ID
	 * @param req           공간 수정 요청 DTO
	 * @param keepUrlsOrder 최종적으로 유지될 이미지 순서 목록 (기존 URL 또는 "new:i" 토큰 포함)
	 * @param newImages     새로 업로드할 이미지 파일 리스트 (S3 업로드는 이 메서드 내부에서 진행됨)
	 * @throws ResponseStatusException  권한이 없는 경우 (403 FORBIDDEN)
	 * @throws IllegalArgumentException 공간 ID, 카테고리/지역/주소/담당자 ID가 유효하지 않거나, 이미지 최소 개수 미달, 지역 권한 불일치, 이미지 순서 불일치 등
	 * @throws DuplicateKeyException    공간명이 다른 기존 공간과 중복될 경우
	 **/
	@Transactional
	public void updateWithImages(Long adminId,
								 Integer spaceId,
								 SpaceCreateRequestDto req,
								 java.util.List<String> keepUrlsOrder,
								 java.util.List<org.springframework.web.multipart.MultipartFile> newImages) {
		// 대상 공간 조회
		Space space = spaceRepository.findById(spaceId)
			.orElseThrow(() -> new IllegalArgumentException("해당 공간이 존재하지 않습니다: " + spaceId));

		// 관리자 권한 체크
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID

		// 마스터 권한({@code ROLE_MASTER, role_id = 0})은 공간 수정 권한이 없음
		if (adminRole.equals(AdminRoleEnum.ROLE_MASTER.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공간 수정 권한이 없습니다.");
		}

		Integer adminRegionId = admin.getAdminRegion().getRegionId(); // 로그인된 관리자의 지역 아이디
		Integer spaceRegionId = space.getRegionId(); // 수정할 지역의 지역 아이디

		// 1차 승인자({@code ROLE_FIRST_APPROVER,role_id = 2})은 담당 지역이 아닐 경우 공간 등록 권한이 없음
		if (adminRole.equals(AdminRoleEnum.ROLE_FIRST_APPROVER.getId()) && !adminRegionId.equals(spaceRegionId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 공간 수정 권한이 없습니다.");
		}

		// 공간 이름 중복 확인 (ID가 현재 공간이 아닌 다른 공간과 이름이 겹치는지 검사)
		String newName = req.getSpaceName() != null ? req.getSpaceName().trim() : null;
		if (newName != null && !newName.equals(space.getSpaceName())) {
			if (spaceRepository.existsBySpaceNameAndSpaceIdNot(newName, spaceId)) {
				throw new IllegalArgumentException("이미 존재하는 공간명입니다.");
			}
			space.setSpaceName(newName);
		}

		// 필수 외래키(Foreign Key)들 엔티티 조회 및 유효성 검증
		// 1) categoryId
		SpaceCategory category = categoryRepository.findByCategoryId(req.getCategoryId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + req.getCategoryId()));

		// 2) regionId
		AdminRegion region = regionRepository.findByRegionId(req.getRegionId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역명입니다: " + req.getRegionId()));

		// 3) locationId
		SpaceLocation location = spaceLocationRepository.findByLocationId(req.getLocationId())
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주소 ID입니다: " + req.getLocationId()));


		// === S3 파일 업로드, 롤백 동기화, 최종 URL 구성 (트랜잭션 내부) ===
		String targetDir = "spaces/" + spaceId;
		java.util.List<String> successfullyUploadedUrls = new java.util.ArrayList<>();
		java.util.List<String> finalUrls = new java.util.ArrayList<>();

		// 1) 신규 파일 S3에 업로드 (트랜잭션 내부에서 실행)
		java.util.List<String> uploadedUrls = newImages.isEmpty()
			? java.util.Collections.emptyList()
			: s3Uploader.uploadAll(newImages, targetDir);

		// 2) S3 롤백 대비 동기화 등록 (트랜잭션 내부이므로 'synchronization is not active' 에러 해결)
		if (!uploadedUrls.isEmpty()) {
			successfullyUploadedUrls.addAll(uploadedUrls);

			// DB 트랜잭션 롤백 시 업로드된 파일을 삭제하도록 동기화 등록
			org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
				new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
					@Override
					public void afterCompletion(int status) {
						if (status == org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK) {
							for (String url : successfullyUploadedUrls) {
								try {
									s3Deleter.deleteByUrl(url);
								} catch (Exception ignored) {
								}
							}
						}
					}
				}
			);
		}

		// 3) 최종 URL 목록(finalUrls) 구성 및 검증 (Controller에서 가져온 복잡한 로직)
		// 3-1) 빈 배열([])이면 전체 삭제 의도로 간주
		if (keepUrlsOrder.isEmpty()) {
			if (!uploadedUrls.isEmpty()) {
				throw new IllegalArgumentException(
					"keepUrlsOrder가 빈 배열인데 새 이미지가 첨부되었습니다. new:i 토큰으로 순서를 명시하세요.");
			}
			// finalUrls는 빈 리스트로 유지
		} else {
			// 3-2) "new:i" 토큰 검증
			java.util.regex.Pattern p = java.util.regex.Pattern.compile("^new:(\\d+)$");
			java.util.Set<Integer> tokenIdx = new java.util.LinkedHashSet<>();

			for (String item : keepUrlsOrder) {
				if (item == null || item.isBlank()) continue;
				java.util.regex.Matcher m = p.matcher(item);
				if (m.matches()) tokenIdx.add(Integer.parseInt(m.group(1)));
			}

			if (uploadedUrls.isEmpty()) {
				if (!tokenIdx.isEmpty()) {
					throw new IllegalArgumentException("새 이미지가 없는데 keepUrlsOrder에 new:i 토큰이 포함되어 있습니다.");
				}
			} else {
				// 토큰 인덱스 집합 검증
				java.util.Set<Integer> expected = new java.util.LinkedHashSet<>();
				for (int i = 0; i < uploadedUrls.size(); i++) expected.add(i);
				if (!tokenIdx.equals(expected)) {
					throw new IllegalArgumentException(
						"keepUrlsOrder의 new:i 토큰 수/인덱스가 업로드한 새 이미지 수와 일치하지 않습니다. " +
							"(expected: new:0..new:" + (uploadedUrls.size() - 1) + ")"
					);
				}
			}

			// 3-3) 최종 리스트 조립 ("new:i" → 업로드 URL 치환, 나머지는 기존 URL로 간주)
			for (String item : keepUrlsOrder) {
				if (item == null || item.isBlank()) continue;
				java.util.regex.Matcher m = p.matcher(item);
				if (m.matches()) {
					int idx = Integer.parseInt(m.group(1));
					if (idx < 0 || idx >= uploadedUrls.size()) {
						throw new IllegalArgumentException("잘못된 new 토큰 인덱스: " + item);
					}
					finalUrls.add(uploadedUrls.get(idx));
				} else {
					finalUrls.add(item); // 기존 URL
				}
			}
		}

		// 4) 최대 개수 제한
		if (finalUrls.size() > 5) {
			throw new IllegalArgumentException("이미지는 최대 5장까지만 설정할 수 있습니다.");
		}

		// 담당자 ID 및 지역 권한 검증
		Long adminIdToAssign = req.getAdminId();

		// 담당자로 지정될 Admin, UserRole, AdminRegion 정보를 함께 로드
		Admin assignedAdmin = adminRepository.findAdminWithRoleAndRegion(adminIdToAssign)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 담당자 ID입니다: " + adminIdToAssign));
		Integer assignedAdminRoleId = assignedAdmin.getUserRole().getRoleId();

		// 1차 승인자 (roleId=2): 권한 검증(해당 담당자의 담당 지역과 일치하는지 검증)
		// 2차 승인자 (roleId=1): 지역 검증을 건너뜀 (전역 권한)
		if (assignedAdminRoleId.equals(AdminRoleEnum.ROLE_FIRST_APPROVER.getId())) {
			Integer requiredRegionId = req.getRegionId();
			Integer assignedAdminRegionId = assignedAdmin.getAdminRegion() != null
				? assignedAdmin.getAdminRegion().getRegionId()
				: null;

			if (assignedAdminRegionId == null || !assignedAdminRegionId.equals(requiredRegionId)) {
				throw new IllegalArgumentException(
					"담당자 지정 불가 - 해당 지역에 대한 권한이 없습니다."
				);
			}
		}

		// Space 본문 필드 업데이트
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

		// === 태그 전량 교체 ===
		tagMapRepository.deleteBySpace(space);
		for (String tagName : req.getTagNames()) {
			// 태그명으로 조회 -> 없으면 예외 발생
			SpaceTag tag = tagRepository.findByTagName(tagName)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다. " + tagName));

			SpaceTagMap map = SpaceTagMap.builder()
				.space(space)
				.tag(tag)
				.regDate(LocalDateTime.now())
				.build();
			tagMapRepository.save(map);
		}

		// === 운영시간 ===
		spaceOperationRepository.deleteBySpaceId(spaceId);

		if (!req.getOperations().isEmpty()) {

			// 1) 유효성 검사: operationFrom 시간이 operationTo 시간보다 이전인지 확인
			boolean isValidTimeRange = req.getOperations().stream()
				.allMatch(o -> {
					// 운영 시간이 열려 있는 경우에만 검사합니다.
					if (Boolean.TRUE.equals(o.getIsOpen())) {
						// from 시간이 to 시간보다 같거나 이후인 경우 (유효하지 않음)
						if (!o.getFrom().isBefore(o.getTo())) {
							return false; // 유효성 검사 실패
						}
					}
					return true; // 유효성 검사 통과 (닫혀 있거나, 시간이 유효함)
				});

			if (!isValidTimeRange) {
				// 유효하지 않은 시간 범위가 발견된 경우 예외 발생
				throw new IllegalArgumentException("운영 시간 '시작 시간(from)'은 '종료 시간(to)'보다 이후이거나 같을 수 없습니다.");
			}

			// 2) 유효성 검사 통과 후 DB 저장
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

		// === 휴무일 ===
		spaceClosedDayRepository.deleteBySpaceId(spaceId);

		if (!req.getClosedDays().isEmpty()) {

			// 1) 유효성 검사: from 날짜가 to 날짜보다 이전인지 확인
			boolean isValidDateRange = req.getClosedDays().stream()
				.allMatch(c -> {
					// from 날짜가 to 날짜보다 크거나 같은 경우 (유효하지 않음)
					if (c.getFrom().isAfter(c.getTo())) {
						return false; // 유효성 검사 실패
					}
					return true; // 유효성 검사 통과
				});

			if (!isValidDateRange) {
				// 유효하지 않은 날짜 범위가 발견된 경우 예외 발생
				throw new IllegalArgumentException("휴무일 '시작일(from)'은 '종료일(to)'보다 이후일 수 없습니다.");
			}

			// 2) 유효성 검사 통과 후 DB 저장
			List<SpaceClosedDay> closedDay = req.getClosedDays().stream().map(c ->
				SpaceClosedDay.builder()
					.space(space)
					.closedFrom(c.getFrom())
					.closedTo(c.getTo())
					.build()
			).toList();

			spaceClosedDayRepository.saveAll(closedDay);
		}

		// === 이미지 처리 ===
		if (finalUrls != null) {

			// 1) 새 상태 스냅샷 (DB 갱신과 삭제 로직 모두에 필요)
			final String newMainUrl = finalUrls.isEmpty() ? null : finalUrls.get(0);
			final java.util.List<String> newGalleryUrls = (finalUrls.size() > 1)
				? finalUrls.subList(1, finalUrls.size())
				: java.util.Collections.emptyList();


			// 2) 기존 상태 스냅샷 (S3 삭제 대상 계산에 필요)
			final String oldMainUrl = space.getSpaceImageUrl();
			final java.util.List<SpaceImage> oldImages = spaceImageRepository.findBySpace(space);

			// 기존 DB의 모든 URL을 수집 (메인 + 상세)
			java.util.Set<String> allExistingUrls = new java.util.HashSet<>();
			if (oldMainUrl != null) {
				allExistingUrls.add(oldMainUrl);
			}
			oldImages.stream()
				.map(SpaceImage::getImageUrl)
				.filter(java.util.Objects::nonNull)
				.forEach(allExistingUrls::add);

			// 3) 삭제 대상 URL 계산: (기존 전체 URL) - (유지될 최종 URL)
			java.util.Set<String> urlsToKeep = new java.util.HashSet<>(finalUrls); // 최종적으로 DB에 저장될 URL 목록
			java.util.List<String> deleteUrls = new java.util.ArrayList<>();

			for (String existingUrl : allExistingUrls) {
				// 기존 URL이 최종 목록(urlsToKeep)에 포함되어 있지 않다면 삭제 대상
				if (!urlsToKeep.contains(existingUrl)) {
					deleteUrls.add(existingUrl);
				}
			}


			// 4) DB 갱신
			// 상세 전량 삭제 후 재삽입(단순 PUT 정책)
			spaceImageRepository.deleteBySpace(space);

			if (finalUrls.isEmpty()) {
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

			// 5) 변경사항 확정 (지연 flush 방지)
			spaceRepository.save(space);
			spaceRepository.flush();

			// 6) 커밋 이후 S3 삭제 (DB 커밋 성공 시에만)
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
	 * - 공간을 삭제하고, 연관된 모든 데이터를 정리하며, S3에 저장된 이미지 파일도 삭제합니다.
	 *
	 * @param adminId 공간 삭제를 요청한 관리자 ID
	 * @param spaceId 삭제할 공간 ID
	 * @throws ResponseStatusException 권한이 없는 경우 (403 FORBIDDEN)
	 * @throws NoSuchElementException  공간 ID에 해당하는 데이터가 없을 경우
	 **/
	@Transactional
	public void deleteSpace(Long adminId, Integer spaceId) {
		// 관리자 권한 체크
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID

		// 마스터 권한({@code ROLE_MASTER, role_id = 0})은 공간 삭제 권한이 없음
		if (adminRole.equals(AdminRoleEnum.ROLE_MASTER.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공간 삭제 권한이 없습니다.");
		}

		// 대상 공간 조회
		Space space = spaceRepository.findById(spaceId)
			.orElseThrow(() -> new IllegalArgumentException("해당 공간이 존재하지 않습니다: " + spaceId));

		Integer adminRegionId = admin.getAdminRegion().getRegionId(); // 로그인된 관리자의 지역 아이디
		Integer spaceRegionId = space.getRegionId(); // 수정할 지역의 지역 아이디

		// 1차 승인자({@code ROLE_FIRST_APPROVER,role_id = 2})은 담당 지역이 아닐 경우 공간 삭제 권한이 없음
		if (adminRole.equals(AdminRoleEnum.ROLE_FIRST_APPROVER.getId()) && !adminRegionId.equals(spaceRegionId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 공간 삭제 권한이 없습니다.");
		}

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
	 * - 새로운 태그(편의시설)를 생성하고 DB에 저장합니다.
	 *
	 * @param adminId 태그 생성을 요청한 관리자 ID
	 * @param tagName 생성할 태그 이름
	 * @return 생성된 {@code SpaceTag} 엔티티
	 * @throws ResponseStatusException  권한이 없는 경우 (403 FORBIDDEN)
	 * @throws IllegalArgumentException 이미 존재하는 태그명인 경우
	 **/
	public SpaceTag createTag(Long adminId, String tagName) {
		// 관리자 권한 체크
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID

		if (adminRole.equals(AdminRoleEnum.ROLE_MASTER.getId())) {
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
	 * 지역 ID로 관리자 리스트 조회
	 * <p>
	 * - 2차 승인자(role_id 1)는 모든 지역에 대해 반환
	 * - 1차 승인자(role_id 2)는 해당 {@code regionId}를 담당 지역으로 가진 관리자만 반환
	 *
	 * @param regionId 조회할 지역 ID
	 * @return 관리자 이름과 ID를 포함한 {@code AdminListResponseDto} 목록
	 */
	public List<AdminListResponseDto> getApproversByRegionId(Integer regionId) {
		return adminRepository.findApproversByRegion(regionId)
			.stream()
			.map(admin -> {
				// roleId에 따른 역할 이름 결정
				String roleName = admin.getUserRole().getRoleId().equals(AdminRoleEnum.ROLE_SECOND_APPROVER.getId()) ? "2차 승인자" : "1차 승인자";

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
