package Team_Mute.back_end.domain.reservation_admin.service;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.exception.UserNotFoundException;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.member.service.EmailService;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation_admin.dto.request.RejectRequestDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ApproveResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.BulkApproveItemResultDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.BulkApproveResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.RejectResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationDetailResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationFilterOptionsResponse;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.UserSummaryDto;
import Team_Mute.back_end.domain.reservation_admin.entity.ReservationLog;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationStatusRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationDetailRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationLogRepository;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import Team_Mute.back_end.global.constants.AdminRoleEnum;
import Team_Mute.back_end.global.constants.ReservationStatusEnum;
import lombok.extern.slf4j.Slf4j;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static Team_Mute.back_end.domain.reservation_admin.util.ReservationApprovalPolicy.isApprovableFor;
import static Team_Mute.back_end.domain.reservation_admin.util.ReservationApprovalPolicy.isRejectableFor;

/**
 * [관리자 예약 관리] 메인 서비스
 * - 예약 검색 및 필터링, 상세 정보 조회, 승인/반려 등의 주요 비즈니스 로직을 담당
 * - 트랜잭션이 필요한 승인 처리는 ReservationApprovalTxService에 위임
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ReservationAdminService {
	private final ReservationApprovalTxService approvalTxService; // 개별 트랜잭션 처리 서비스
	private final RservationListAllService rservationListAllService; // 리스트 조회 및 필터링 서비스
	private final AdminReservationRepository adminReservationRepository;
	private final AdminReservationStatusRepository adminStatusRepository;
	private final SpaceRepository spaceRepository;
	private final AdminRepository adminRepository;
	private final ReservationLogRepository reservationLogRepository;
	private final ReservationDetailRepository reservationDetailRepository;
	private final EmailService emailService;

	// 생성자
	public ReservationAdminService(
		ReservationApprovalTxService approvalTxService,
		RservationListAllService rservationListAllService, // 리스트 조회 및 필터링 서비스
		AdminReservationRepository adminReservationRepository,
		AdminReservationStatusRepository adminStatusRepository,
		SpaceRepository spaceRepository,
		AdminRepository adminRepository,
		ReservationLogRepository reservationLogRepository,
		ReservationDetailRepository reservationDetailRepository,
		EmailService emailService
	) {
		this.approvalTxService = approvalTxService;
		this.rservationListAllService = rservationListAllService;
		this.adminReservationRepository = adminReservationRepository;
		this.adminStatusRepository = adminStatusRepository;
		this.spaceRepository = spaceRepository;
		this.adminRepository = adminRepository;
		this.reservationLogRepository = reservationLogRepository;
		this.reservationDetailRepository = reservationDetailRepository;
		this.emailService = emailService;
	}

	/**
	 * 일괄 승인 에러 메세지
	 * - 발생한 예외 객체를 클라이언트 응답용 메시지로 변환(특히 ResponseStatusException의 reason 필드 활용)
	 *
	 * @param ex 발생한 Exception
	 * @return 클라이언트에게 보여줄 메시지 String
	 */
	private String toClientMessage(Exception ex) {
		if (ex instanceof org.springframework.web.server.ResponseStatusException rse) {
			return rse.getReason() != null ? rse.getReason() : rse.getMessage();
		}
		return ex.getMessage();
	}

	/**
	 * 1차 승인 + 2차 승인
	 * - 여러 건의 예약을 관리자 권한에 따라 1차 또는 2차로 승인
	 * - 각 승인 건별로 별도의 트랜잭션 보장을 위해 NOT_SUPPORTED 전파 레벨 사용
	 *
	 * @param adminId        승인 요청 관리자 ID (Long)
	 * @param reservationIds 승인 대상 예약 ID 목록 (Long List)
	 * @return 일괄 처리 결과 DTO (성공/실패 건수 및 개별 결과 포함)
	 */
	@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
	public BulkApproveResponseDto approveReservation(Long adminId, java.util.List<Long> reservationIds) {
		java.util.List<Long> ids = reservationIds.stream().distinct().toList(); // 중복 제거(선택)
		BulkApproveResponseDto resp = new BulkApproveResponseDto();
		resp.setTotal(ids.size());

		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));

		Integer roleId = admin.getUserRole().getRoleId();

		// 1차 승인자일 경우 1차 승인 처리
		if (AdminRoleEnum.ROLE_FIRST_APPROVER.getId().equals(roleId)) {
			for (Long id : ids) {
				try {
					ApproveResponseDto r = approvalTxService.approveFirstTx(adminId, id); // 개별 Tx 서비스 호출
					resp.add(new BulkApproveItemResultDto(id, true, r.getMessage()));
					resp.setSuccessCount(resp.getSuccessCount() + 1);
				} catch (Exception ex) {
					resp.add(new BulkApproveItemResultDto(id, false, toClientMessage(ex)));
					resp.setFailureCount(resp.getFailureCount() + 1);
				}
			}
		}
		// 2차 승인자일 경우 2차 승인 처리
		else if (AdminRoleEnum.ROLE_SECOND_APPROVER.getId().equals(roleId)) {
			for (Long id : ids) {
				try {
					ApproveResponseDto r = approvalTxService.approveSecondTx(adminId, id); // 개별 Tx 서비스 호출
					String finalMsg = r.getMessage(); // "2차 승인 완료"

					resp.add(new BulkApproveItemResultDto(id, true, finalMsg));
					resp.setSuccessCount(resp.getSuccessCount() + 1);
				} catch (Exception ex) {
					resp.add(new BulkApproveItemResultDto(id, false, toClientMessage(ex)));
					resp.setFailureCount(resp.getFailureCount() + 1);
				}
			}
		}
		return resp;
	}

	/**
	 * 예약 반려
	 * - 예약을 반려 상태로 변경하고 로그를 기록 및 이메일 전송을 진행
	 *
	 * @param adminId       반려 요청 관리자 ID (Long)
	 * @param reservationId 반려 대상 예약 ID (Long)
	 * @param requestDto    반려 사유 DTO
	 * @return 반려 결과 DTO
	 * @throws ResponseStatusException 권한, 상태 불일치, 지역 불일치 등 오류 발생 시
	 */
	@Transactional
	public RejectResponseDto rejectReservation(Long adminId, Long reservationId, RejectRequestDto requestDto) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		// 예약 엔티티 조회
		Reservation reservation = adminReservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

		// 관리자의 역할 ID
		Integer roleId = admin.getUserRole().getRoleId();
		Integer reservationRegionId = reservation.getSpace().getRegionId(); // 예약된 공간의 지역ID 조회

		// 현재 승인 상태 ID
		Integer currentStatusId = reservation.getReservationStatusId().getReservationStatusId();

		// 권한 체크 (1차 또는 2차 승인자)
		if (roleId.equals(AdminRoleEnum.ROLE_SECOND_APPROVER.getId()) || roleId.equals(AdminRoleEnum.ROLE_FIRST_APPROVER.getId())) {
			// 1차 승인자일 경우 담당 지역 체크 및 1차 승인 완료 건 반려 불가 체크
			if (roleId.equals(AdminRoleEnum.ROLE_FIRST_APPROVER.getId())) {
				Integer adminRegionId = admin.getAdminRegion().getRegionId(); // 관리자의 담당 지역 ID
				if (!reservationRegionId.equals(adminRegionId)) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"해당 지역의 반려 권한이 없습니다 담당 지역인지 확인하세요");
				}
				// ReservationStatusEnum.getId()와 currentStatusId를 비교
				if (currentStatusId.equals(ReservationStatusEnum.WAITING_SECOND_APPROVAL.getId())) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"이미 1차 승인이 완료된 예약입니다.");
				}
			}
			// 이미 최종 상태(최종 승인 완료/반려/이용 완료/취소)인지 확인
			// 데이터베이스의 상태 ID를 기반으로 이미 최종 상태인 경우를 확인
			// 3: 최종 승인 완료, 4: 반려됨, 5: 이용 완료, 6: 취소됨
			if (currentStatusId.equals(ReservationStatusEnum.FINAL_APPROVAL.getId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 최종 승인 완료된 예약입니다.");
			} else if (currentStatusId.equals(ReservationStatusEnum.REJECTED_STATUS.getId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 반려된 예약입니다.");
			} else if (currentStatusId.equals(ReservationStatusEnum.USER_COMPLETED.getId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 이용 완료된 예약입니다.");
			} else if (currentStatusId.equals(ReservationStatusEnum.CANCELED_STATUS.getId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자에 의해 취소된 예약입니다.");
			}
			String rejectionReason = requestDto.getRejectionReason();

			if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반려 사유는 필수 입력 항목입니다.");
			}

			// 반려 상태 엔티티 조회
			ReservationStatus rejectedStatus = adminStatusRepository.findById(
					ReservationStatusEnum.REJECTED_STATUS.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rejected status not found"));

			// 예약 상태 업데이트 및 로그 기록
			reservation.setReservationStatusId(rejectedStatus);
			reservation.setUpdDate(LocalDateTime.now());

			// 반려 사유를 로그 테이블에 저장
			ReservationLog reservationLog = new ReservationLog();
			reservationLog.setReservation(reservation);
			reservationLog.setChangedStatus(rejectedStatus);
			reservationLog.setMemo(rejectionReason);
			reservationLog.setRegDate(LocalDateTime.now());

			reservationLogRepository.save(reservationLog);

			// 반려 성공 시 Email 발송 시도 (실패하면 반려도 실패)
			try {
				emailService.sendMailForReservationAdmin(
					reservation,
					ReservationStatusEnum.REJECTED_STATUS.getId(),
					rejectionReason
				);
			} catch (MailException e) {
				throw new RuntimeException(e.getMessage(), e);
			}

			return new RejectResponseDto(
				reservation.getReservationId(),
				reservation.getReservationStatusId().getReservationStatusName(),
				rejectedStatus.getReservationStatusName(),
				LocalDateTime.now(),
				rejectionReason,
				"반려 완료"
			);
		} else {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "반려 권한이 없습니다.");
		}
	}

	/**
	 * 예약 상세 조회
	 * - 특정 예약의 상세 정보를 조회하고, 현재 관리자의 권한을 기준으로 승인/반려 가능 여부를 계산합니다.
	 *
	 * @param adminId       현재 로그인된 관리자 ID (Long)
	 * @param reservationId 조회할 예약 ID (Long)
	 * @return 예약 상세 정보 DTO
	 */
	@Transactional
	public ReservationDetailResponseDto getByReservationId(Long adminId, Long reservationId) {
		Reservation reservation = reservationDetailRepository.findById(reservationId)
			.orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

		// Space 조회: Reservation에 연관관계가 없으므로 spaceId로 별도 조회
		Space s = null;
		try {
			Integer spaceId = reservation.getSpaceId().getSpaceId();
			if (spaceId != null) {
				s = spaceRepository.findById(spaceId).orElse(null);
			}
		} catch (Exception ignored) {
		}

		String spaceName = (s == null) ? null : s.getSpaceName();

		// User 매핑 (LAZY 로딩을 통해 UserSummaryDto 생성)
		User u = reservation.getUserId();
		var user = (u == null) ? null : new UserSummaryDto(
			u.getUserId(),
			u.getUserName(),
			u.getUserEmail(),
			(u.getUserCompany() != null) ? u.getUserCompany().getCompanyName() : null
		);

		// Status 매핑
		String statusName = (reservation.getReservationStatusId() != null)
			? reservation.getReservationStatusId().getReservationStatusName()
			: null;

		// 승인 가능 여부(승인 버튼 활성화 여부) 계산
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);
		Integer roleId = admin.getUserRole().getRoleId();

		Integer reservationRegionId = reservation.getSpace().getRegionId(); // 예약된 공간의 지역ID 조회

		// 관리자 담당 지역 ID
		Integer adminRegionId = null;
		if (admin.getAdminRegion() != null) {
			adminRegionId = admin.getAdminRegion().getRegionId();
		}

		// 승인 가능 여부(승인 버튼 활성화 여부)
		boolean isApprovable = isApprovableFor(reservationRegionId, adminRegionId, roleId, statusName);

		// 반려 가능 여부(반려 버튼 활성화 여부)
		boolean isRejectable = isRejectableFor(reservationRegionId, adminRegionId, roleId, statusName);

		return new ReservationDetailResponseDto(
			reservation.getReservationId(),
			spaceName,
			user,
			reservation.getReservationPurpose(),
			reservation.getReservationHeadcount(),
			reservation.getReservationFrom(),
			reservation.getReservationTo(),
			reservation.getOrderId(),
			statusName,
			isApprovable,
			isRejectable,
			reservation.getReservationStatusId().getReservationStatusId()
		);
	}

	/**
	 * 1) 필터 옵션 조회: 상태 조회
	 **/
	public List<ReservationFilterOptionsResponse.StatusOptionDto> getStatusOptions() {
		return adminStatusRepository
			.findAll(Sort.by(Sort.Direction.ASC, "reservationStatusId"))
			.stream()
			.map(this::toStatusOption)
			.collect(Collectors.toList());
	}

	/**
	 * 2) 필터 옵션 조회: 긴급 및 신한 플래그 조회
	 **/
	public List<ReservationFilterOptionsResponse.FlagOptionDto> getFlagOptions() {
		return List.of(
			ReservationFilterOptionsResponse.FlagOptionDto.builder().key("isShinhan").label("신한 예약 보기").build(),
			ReservationFilterOptionsResponse.FlagOptionDto.builder().key("isEmergency").label("긴급 예약 보기").build()
		);
	}

	/**
	 * ReservationStatus 엔티티를 필터링 옵션 응답 DTO로 변환
	 *
	 * @param status ReservationStatus 엔티티
	 * @return 필터 옵션 DTO (ID는 Long 타입)
	 */
	private ReservationFilterOptionsResponse.StatusOptionDto toStatusOption(ReservationStatus status) {
		return ReservationFilterOptionsResponse.StatusOptionDto.builder()
			.id(status.getReservationStatusId())
			.label(status.getReservationStatusName())
			.build();
	}

	/**
	 * 복합 검색
	 * - 조건에 맞는 예약 리스트를 검색하고 필터링/페이징 처리합니다.
	 * - DB에서 모든 데이터를 가져와 서비스 레이어에서 필터링 및 페이징 처리
	 *
	 * @param adminId     관리자 ID (Long)
	 * @param keyword     키워드 (예약자명/공간명)
	 * @param regionId    지역 ID (Integer)
	 * @param statusId    상태 ID (Long)
	 * @param isShinhan   신한 예약 여부
	 * @param isEmergency 긴급 예약 여부
	 * @param pageable    페이징 정보
	 * @return 필터링 및 페이징된 예약 리스트 DTO Page
	 */
	public Page<ReservationListResponseDto> searchReservations(
		Long adminId,
		String keyword,
		Integer regionId,
		Integer statusId,
		Boolean isShinhan,
		Boolean isEmergency,
		Pageable pageable
	) {
		// 1. 가공된 전체 데이터 가져오기 (DB 조회 + 관리자 권한/지역 필터링 + 정렬 포함)
		List<Reservation> allReservations = adminReservationRepository.findAll();
		Admin admin = adminRepository.findById(adminId).orElseThrow(UserNotFoundException::new);
		List<ReservationListResponseDto> allDtos = rservationListAllService.getReservationListAll(allReservations,
			admin);

		// 2. 복합 조건에 따른 필터링 (Stream API 활용)
		List<ReservationListResponseDto> filteredList = allDtos.stream()
			.filter(dto -> {
				// 키워드(예약자명, 공간명) 필터링 (키워드가 있을 경우에만 적용)
				boolean keywordMatch = true;
				if (keyword != null && !keyword.isBlank()) {
					String norm = normalizeKeyword(keyword);
					String user = normalizeNullable(dto.getUserName());
					String space = normalizeNullable(dto.getSpaceName());
					keywordMatch = (user.contains(norm) || space.contains(norm));
				}

				// 지역 ID 필터링 (지역 ID가 있을 경우에만 적용)
				boolean regionMatch =
					(regionId == null) || (dto.getRegionId() != null && dto.getRegionId().equals(regionId));

				// 상태 ID 필터링 (상태 ID가 있을 경우에만 적용)
				boolean statusMatch =
					(statusId == null) || (dto.getStatusId() != null && dto.getStatusId().equals(statusId));

				// 신한 예약 플래그 필터링 (플래그가 있을 경우에만 적용)
				boolean shinhanMatch = (isShinhan == null) || (dto.getIsShinhan().equals(isShinhan));

				// 긴급 예약 플래그 필터링 (플래그가 있을 경우에만 적용)
				boolean emergencyMatch = (isEmergency == null) || (dto.getIsEmergency().equals(isEmergency));

				// 모든 조건이 true일 때만 반환
				return keywordMatch && regionMatch && statusMatch && shinhanMatch && emergencyMatch;
			})
			.toList();

		// 3. 수동으로 페이징 처리(DB Pageable 대신 메모리 List 기반)
		int start = (int) pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), filteredList.size());
		List<ReservationListResponseDto> pagedContent;
		if (start > filteredList.size()) {
			pagedContent = List.of();
		} else {
			pagedContent = filteredList.subList(start, end);
		}

		// 4. 페이징 정보가 담긴 PageImpl 반환
		return new PageImpl<>(pagedContent, pageable, filteredList.size());
	}

	/**
	 * 검색 키워드를 정규화(NFC, 소문자)하여 검색 효율성을 높임
	 */
	private String normalizeKeyword(String s) {
		String trimmed = s == null ? "" : s.trim();
		// 한글/악센트 문자 정규화 + 대소문자 정리
		String nfc = Normalizer.normalize(trimmed, Normalizer.Form.NFC);
		return nfc.toLowerCase(Locale.ROOT);
	}

	/**
	 * Nullable 문자열을 안전하게 정규화하여 반환
	 */
	private String normalizeNullable(String s) {
		if (s == null)
			return "";
		String nfc = Normalizer.normalize(s, Normalizer.Form.NFC);
		return nfc.toLowerCase(Locale.ROOT);
	}
}
