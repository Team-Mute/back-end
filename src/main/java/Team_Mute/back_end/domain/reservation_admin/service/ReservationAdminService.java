package Team_Mute.back_end.domain.reservation_admin.service;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.exception.UserNotFoundException;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.member.repository.UserCompanyRepository;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
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
import Team_Mute.back_end.domain.reservation_admin.repository.AdminPrevisitReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationStatusRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationDetailRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationLogRepository;
import Team_Mute.back_end.domain.reservation_admin.util.EmergencyEvaluator;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static Team_Mute.back_end.domain.reservation_admin.util.ReservationApprovalPolicy.isApprovableFor;
import static Team_Mute.back_end.domain.reservation_admin.util.ReservationApprovalPolicy.isRejectableFor;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ReservationAdminService {
	private final ReservationApprovalTxService approvalTxService;
	private final RservationListAllService rservationListAllService;
	private final AdminReservationRepository adminReservationRepository;
	private final AdminPrevisitReservationRepository adminPrevisitRepository;
	private final AdminReservationStatusRepository adminStatusRepository;
	private final SpaceRepository spaceRepository;
	private final UserRepository userRepository;
	private final AdminRepository adminRepository;
	private final UserCompanyRepository userCompanyRepository;
	private final ReservationLogRepository reservationLogRepository;
	private final ReservationDetailRepository reservationDetailRepository;
	private final EmergencyEvaluator emergencyEvaluator;

	public ReservationAdminService(
		ReservationApprovalTxService approvalTxService,
		RservationListAllService rservationListAllService,
		AdminReservationRepository adminReservationRepository,
		AdminPrevisitReservationRepository adminPrevisitRepository,
		AdminReservationStatusRepository adminStatusRepository,
		SpaceRepository spaceRepository,
		UserRepository userRepository,
		AdminRepository adminRepository,
		UserCompanyRepository userCompanyRepository,
		ReservationLogRepository reservationLogRepository,
		ReservationDetailRepository reservationDetailRepository,
		EmergencyEvaluator emergencyEvaluator
	) {
		this.approvalTxService = approvalTxService;
		this.rservationListAllService = rservationListAllService;
		this.adminReservationRepository = adminReservationRepository;
		this.adminPrevisitRepository = adminPrevisitRepository;
		this.adminStatusRepository = adminStatusRepository;
		this.spaceRepository = spaceRepository;
		this.userRepository = userRepository;
		this.adminRepository = adminRepository;
		this.userCompanyRepository = userCompanyRepository;
		this.reservationLogRepository = reservationLogRepository;
		this.reservationDetailRepository = reservationDetailRepository;
		this.emergencyEvaluator = emergencyEvaluator;
	}

	// 일괄 승인 에러 메세지
	private String toClientMessage(Exception ex) {
		if (ex instanceof org.springframework.web.server.ResponseStatusException rse) {
			return rse.getReason() != null ? rse.getReason() : rse.getMessage();
		}
		return ex.getMessage();
	}

	// ===== 1차 일괄 승인 =====
	@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
	public BulkApproveResponseDto approveFirstBulk(Long adminId, java.util.List<Long> reservationIds) {
		java.util.List<Long> ids = reservationIds.stream().distinct().toList(); // 중복 제거(선택)
		BulkApproveResponseDto resp = new BulkApproveResponseDto();
		resp.setTotal(ids.size());

		for (Long id : ids) {
			try {
				ApproveResponseDto r = approvalTxService.approveFirstTx(adminId, id); // ← Tx 전용 서비스 호출
				resp.add(new BulkApproveItemResultDto(id, true, r.getMessage()));
				resp.setSuccessCount(resp.getSuccessCount() + 1);
			} catch (Exception ex) {
				resp.add(new BulkApproveItemResultDto(id, false, toClientMessage(ex)));
				resp.setFailureCount(resp.getFailureCount() + 1);
			}
		}
		return resp;
	}

	// ===== 2차 일괄 승인 =====
	@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
	public BulkApproveResponseDto approveSecondBulk(Long adminId, java.util.List<Long> reservationIds) {
		java.util.List<Long> ids = reservationIds.stream().distinct().toList();
		BulkApproveResponseDto resp = new BulkApproveResponseDto();
		resp.setTotal(ids.size());

		for (Long id : ids) {
			try {
				ApproveResponseDto r = approvalTxService.approveSecondTx(adminId, id); // ← Tx 전용 서비스 호출
				resp.add(new BulkApproveItemResultDto(id, true, r.getMessage()));
				resp.setSuccessCount(resp.getSuccessCount() + 1);
			} catch (Exception ex) {
				resp.add(new BulkApproveItemResultDto(id, false, toClientMessage(ex)));
				resp.setFailureCount(resp.getFailureCount() + 1);
			}
		}
		return resp;
	}

	//  ================== 예약 반려 ==================
	public static final Long REJECTED_STATUS_ID = 4L; // 반려 상태

	@Transactional
	public RejectResponseDto rejectReservation(Long adminId, Long reservationId, RejectRequestDto requestDto) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());

		if (roleId.equals(1L) || roleId.equals(1L)) {
			// 예약 엔티티 조회
			Reservation reservation = adminReservationRepository.findById(reservationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

			// 이미 최종 상태인 경우 예외 처리
			Long currentStatusId = reservation.getReservationStatusId().getReservationStatusId();

			// 데이터베이스의 상태 ID를 기반으로 이미 최종 상태인 경우를 확인
			// 3: 최종 승인 완료, 4: 반려됨, 5: 사용 완료, 6: 취소됨
			if (currentStatusId.equals(3L)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 최종 승인 완료된 예약입니다.");
			} else if (currentStatusId.equals(4L)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 반려된 예약입니다.");
			} else if (currentStatusId.equals(5L)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 완료된 예약입니다.");
			} else if (currentStatusId.equals(6L)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자에 의해 취소된 예약입니다.");
			}
			String rejectionReason = requestDto.getRejectionReason();

			if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반려 사유는 필수 입력 항목입니다.");
			}

			// 반려 상태 엔티티 조회
			ReservationStatus rejectedStatus = adminStatusRepository.findById(REJECTED_STATUS_ID)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rejected status not found"));

			// 예약 및 사전답사 상태 변경
			reservation.setReservationStatusId(rejectedStatus);
			reservation.setUpdDate(LocalDateTime.now());

			if (reservation.getPrevisitReservations() != null) {
				for (PrevisitReservation previsit : reservation.getPrevisitReservations()) {
					previsit.setReservationStatusId(rejectedStatus.getReservationStatusId());
					previsit.setUpdDate(LocalDateTime.now());
				}
				adminPrevisitRepository.saveAll(reservation.getPrevisitReservations());
			}

			// 반려 사유를 로그 테이블에 저장
			ReservationLog log = new ReservationLog();
			log.setReservation(reservation);
			log.setChangedStatus(rejectedStatus);
			log.setMemo(rejectionReason);
			log.setRegDate(LocalDateTime.now());

			reservationLogRepository.save(log);

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

	// ================== 예약 리스트 조회 ==================
	public Page<ReservationListResponseDto> getAllReservations(Long adminId, Pageable pageable) {
		// 관리자 권한
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());

		// 예약 페이지 로딩
		Page<Reservation> page = adminReservationRepository.findAll(pageable);
		List<Reservation> reservations = page.getContent();
		List<ReservationListResponseDto> content = rservationListAllService.getReservationListAll(reservations, roleId);

		if (reservations.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, 0);
		}


		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	// ================== 예약 상세 조회 ==================
	public ReservationDetailResponseDto getByReservationId(Long adminId, Long reservationId) {
		Reservation r = reservationDetailRepository.findById(reservationId)
			.orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

		// --- Space 조회: Reservation에 연관관계가 없으므로 spaceId로 별도 조회
		Space s = null;
		try {
			Integer spaceId = r.getSpaceId().getSpaceId();        // <-- Reservation의 spaceId(Long) 필드명에 맞춰 수정
			if (spaceId != null) {
				s = spaceRepository.findById(spaceId).orElse(null);
			}
		} catch (Exception ignored) {
		}

		String spaceName = (s == null) ? null : s.getSpaceName();

		// User 매핑
		User u = r.getUserId();
		var user = (u == null) ? null : new UserSummaryDto(
			u.getUserId(),
			u.getUserName(),
			u.getUserEmail(),
			u.getUserPhone(),
			(u.getUserCompany() != null) ? u.getUserCompany().getCompanyName() : null
		);

		// --- Status 매핑
		String statusName = (r.getReservationStatusId() != null)
			? r.getReservationStatusId().getReservationStatusName()
			: null;

		// --- 승인 가능 여부(승인 버튼 활성화 여부) ---
		// 관리자 권한
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);
		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());
		boolean isApprovable = isApprovableFor(roleId, statusName);

		// 반려 가능 여부(반려 버튼 활성화 여부)
		boolean isRejectable = isRejectableFor(roleId, statusName);

		return new ReservationDetailResponseDto(
			r.getReservationId(),
			spaceName,
			user,
			r.getReservationPurpose(),
			r.getReservationHeadcount(),
			r.getReservationFrom(),
			r.getReservationTo(),
			r.getOrderId(),
			statusName,
			isApprovable,
			isRejectable
		);
	}

	// 필터 옵션 조회 -> 예약 관리 필터링 드롭다운 구성을 위함
	public ReservationFilterOptionsResponse getFilterOptions() {
		// 1) 상태: DB에서 정렬 조회 후 매핑
		List<ReservationFilterOptionsResponse.StatusOptionDto> statuses = adminStatusRepository
			.findAll(Sort.by(Sort.Direction.ASC, "reservationStatusId"))
			.stream()
			.map(this::toStatusOption)
			.collect(Collectors.toList());

		// 2) flags: 하드코딩
		List<ReservationFilterOptionsResponse.FlagOptionDto> flags = List.of(
			ReservationFilterOptionsResponse.FlagOptionDto.builder().key("isShinhan").label("신한 예약").build(),
			ReservationFilterOptionsResponse.FlagOptionDto.builder().key("isEmergency").label("긴급 예약").build()
		);

		return ReservationFilterOptionsResponse.builder()
			.statuses(statuses)
			.flags(flags)
			.build();
	}

	private ReservationFilterOptionsResponse.StatusOptionDto toStatusOption(ReservationStatus status) {
		return ReservationFilterOptionsResponse.StatusOptionDto.builder()
			.id(status.getReservationStatusId())
			.label(status.getReservationStatusName())
			.build();
	}

	// ========================== 필터링 ==============================
	public Page<ReservationListResponseDto> getFilteredReservations(Long adminId, String filterOptions, Pageable pageable) {
		// 관리자 권한
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());

		// 예약 페이지 로딩
		Page<Reservation> page = adminReservationRepository.findAll(pageable);
		List<Reservation> reservations = page.getContent();
		List<ReservationListResponseDto> allData = rservationListAllService.getReservationListAll(reservations, roleId);

		Stream<ReservationListResponseDto> stream = allData.stream();

		switch (filterOptions) {
			case "신한 예약" -> stream = stream.filter(item -> Boolean.TRUE.equals(item.isShinhan));
			case "긴급 예약" -> stream = stream.filter(item -> Boolean.TRUE.equals(item.isEmergency));
			default ->
				// 상태명 필터 (예: "1차 승인 대기", "취소됨")
				stream = stream.filter(item -> filterOptions.equals(item.reservationStatusName));
		}

		List<ReservationListResponseDto> filtered = stream.toList();

		// 3) 페이징
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), filtered.size());
		List<ReservationListResponseDto> content =
			(start >= filtered.size()) ? Collections.emptyList() : filtered.subList(start, end);

		return new PageImpl<>(content, pageable, filtered.size());
	}

}
