package Team_Mute.back_end.domain.reservation_admin.service;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.entity.UserCompany;
import Team_Mute.back_end.domain.member.exception.UserNotFoundException;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.member.repository.UserCompanyRepository;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation_admin.dto.request.RejectRequestDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ApproveResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.PrevisitItemResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.RejectResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationDetailResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.UserSummaryDto;
import Team_Mute.back_end.domain.reservation_admin.entity.ReservationLog;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminPrevisitReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationStatusRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationDetailRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationLogRepository;
import Team_Mute.back_end.domain.reservation_admin.util.EmergencyEvaluator;
import Team_Mute.back_end.domain.reservation_admin.util.ShinhanGroupUtils;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ReservationAdminService {

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

	// ================== 상태 이름 상수 ==================
	private static final String STATUS_FIRST_PENDING = "1차 승인 대기";
	private static final String STATUS_SECOND_PENDING = "2차 승인 대기";
	private static final String STATUS_FINAL_APPROVED = "최종 승인 완료";

	public ReservationAdminService(
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

	// name -> id 캐시 (간단 캐시)
	private final Map<String, Long> statusIdCache = new HashMap<>();

	private Long statusId(String name) {
		return statusIdCache.computeIfAbsent(name, key ->
			adminStatusRepository.findByReservationStatusName(key)
				.map(ReservationStatus::getReservationStatusId)
				.orElseThrow(() -> new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Unknown reservation status: " + key))
		);
	}

	// ================== 1차 승인 ==================
	@Transactional
	public ApproveResponseDto approveFirst(Long adminId, Long reservationId) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());

		Reservation r = adminReservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

		Long fromStatusId = r.getReservationStatusId().getReservationStatusId();
		String fromStatus = adminStatusRepository.findById(fromStatusId)
			.map(ReservationStatus::getReservationStatusName).orElse("UNKNOWN");

		// 허용 전이: FIRST_PENDING -> SECOND_PENDING
		if (!STATUS_FIRST_PENDING.equals(fromStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Cannot approve second step from status: " + fromStatus);
		}

		if (roleId.equals(0L) || roleId.equals(1L) || roleId.equals(2L)) {
			ReservationStatus toStatus = adminStatusRepository.findById(statusId(STATUS_SECOND_PENDING))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found"));

			// 예약 테이블 상태 변경
			r.setReservationStatusId(toStatus);
			r.setUpdDate(LocalDateTime.now());

			// 사전답사 테이블 상태 변경
			if (r.getPrevisitReservations() != null) {
				for (PrevisitReservation previsit : r.getPrevisitReservations()) {
					previsit.setReservationStatusId(toStatus.getReservationStatusId());
					previsit.setUpdDate(LocalDateTime.now());
				}
				adminPrevisitRepository.saveAll(r.getPrevisitReservations());
			}
		} else {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인 권한이 없습니다.");
		}

		return new ApproveResponseDto(reservationId, fromStatus, STATUS_SECOND_PENDING, LocalDateTime.now(), "1차 승인 완료");
	}

	// ================== 2차 승인 ==================
	@Transactional
	public ApproveResponseDto approveSecond(Long adminId, Long reservationId) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());

		Reservation r = adminReservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

		Long fromStatusId = r.getReservationStatusId().getReservationStatusId();
		String fromStatus = adminStatusRepository.findById(fromStatusId)
			.map(ReservationStatus::getReservationStatusName).orElse("UNKNOWN");

		// 허용 전이: FIRST_PENDING or SECOND_PENDING -> FINAL_APPROVED
		if (!STATUS_FIRST_PENDING.equals(fromStatus) && !STATUS_SECOND_PENDING.equals(fromStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Cannot approve second step from status: " + fromStatus);
		}

		if (roleId.equals(0L) || roleId.equals(1L)) {
			ReservationStatus toStatus = adminStatusRepository.findById(statusId(STATUS_FINAL_APPROVED))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found"));

			// 예약 테이블 상태 변경
			r.setReservationStatusId(toStatus);
			r.setUpdDate(LocalDateTime.now());

			// 사전답사 테이블 상태 변경
			if (r.getPrevisitReservations() != null) {
				for (PrevisitReservation previsit : r.getPrevisitReservations()) {
					previsit.setReservationStatusId(toStatus.getReservationStatusId());
					previsit.setUpdDate(LocalDateTime.now());
				}
				adminPrevisitRepository.saveAll(r.getPrevisitReservations());
			}
		} else {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인 권한이 없습니다.");
		}

		return new ApproveResponseDto(reservationId, fromStatus, STATUS_FINAL_APPROVED, LocalDateTime.now(), "2차 승인 완료");
	}

	// 반려 상태 ID를 상수로 정의하거나, DB에서 동적으로 가져오는 메서드를 사용
	private static final Long REJECTED_STATUS_ID = 4L;

	@Transactional
	public RejectResponseDto rejectReservation(Long adminId, Long reservationId, RejectRequestDto requestDto) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(UserNotFoundException::new);

		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());

		if (roleId.equals(0L) || roleId.equals(1L) || roleId.equals(1L)) {
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
			log.info("디버그: 받은 반려 사유 -> '{}'", rejectionReason);
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
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인 권한이 없습니다.");
		}
	}

	// ================== 예약 리스트 조회 ==================
	public Page<ReservationListResponseDto> getAllReservations(Pageable pageable) {
		// 예약 페이지 로딩
		Page<Reservation> page = adminReservationRepository.findAll(pageable);
		List<Reservation> reservations = page.getContent();

		if (reservations.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, 0);
		}

		// 예약/사전답사에서 쓰일 상태ID 수집
		Set<Long> statusIds = reservations.stream()
			.map(r -> r.getReservationStatus().getReservationStatusId())
			.collect(Collectors.toSet());

		// 사전답사 일괄 로딩(예약ID IN (...))
		List<Long> reservationIds = reservations.stream()
			.map(Reservation::getReservationId)
			.toList();

		List<PrevisitReservation> previsitList = adminPrevisitRepository.findByReservation_ReservationIdIn(reservationIds);

		statusIds.addAll(previsitList.stream()
			.map(PrevisitReservation::getReservationStatusId)
			.collect(Collectors.toSet()));

		// 상태ID → 상태명 맵
		Map<Long, String> statusNameById = adminStatusRepository.findAllById(statusIds).stream()
			.collect(Collectors.toMap(
				ReservationStatus::getReservationStatusId,
				ReservationStatus::getReservationStatusName
			));

		// 공간/유저 이름 배치 조회
		Set<Integer> spaceIds = reservations.stream().map(r -> r.getSpace().getSpaceId()).collect(Collectors.toSet());
		Set<Long> userIds = reservations.stream().map(r -> r.getUser().getUserId()).collect(Collectors.toSet());

		// spaceId -> spaceName 맵
		Map<Integer, String> spaceNameById = spaceRepository.findAllById(spaceIds).stream()
			.collect(Collectors.toMap(Space::getSpaceId, Space::getSpaceName));

		// userId -> userName 맵
		Map<Long, String> userNameById = userRepository.findAllById(userIds).stream()
			.collect(Collectors.toMap(User::getUserId, User::getUserName));

		// 사전답사들을 예약ID로 그룹핑
		Map<Long, List<PrevisitReservation>> previsitMap = previsitList.stream()
			.collect(Collectors.groupingBy(p -> p.getReservation().getReservationId()));

		// 유저 목록(이름 + 연결된 회사 엔티티 LAZY) 조회
		List<User> users = userRepository.findAllById(userIds);

		// 회사 id 모으기 (LAZY 지연로딩이지만 페이지당 5~6건이면 부담 적음)
		Set<Integer> companyIds = users.stream()
			.map(u -> u.getUserCompany() != null ? u.getUserCompany().getCompanyId() : null)
			.filter(java.util.Objects::nonNull)
			.collect(java.util.stream.Collectors.toSet());

		// companyId -> companyName 맵 생성 (요게 companyNameById)
		Map<Integer, String> companyNameById = userCompanyRepository.findAllById(companyIds).stream()
			.collect(Collectors.toMap(UserCompany::getCompanyId, UserCompany::getCompanyName));

		// userId -> isShinhan 맵
		Map<Integer, Boolean> isShinhanByUserId =
			ShinhanGroupUtils.buildIsShinhanByUserId(users, companyNameById);

		// DTO 변환
		List<ReservationListResponseDto> content = reservations.stream()
			.map(r -> {
				// 사전답사 DTO 변환
				List<PrevisitItemResponseDto> previsitDtos = previsitMap
					.getOrDefault(r.getReservationId(), Collections.emptyList())
					.stream()
					.map(p -> PrevisitItemResponseDto.from(
						p,
						statusNameById.getOrDefault(p.getReservationStatusId(), "UNKNOWN")
					))
					.toList();

				String statusName = statusNameById.getOrDefault(r.getReservationStatus().getReservationStatusId(), "UNKNOWN");
				String spaceName = spaceNameById.getOrDefault(r.getSpace().getSpaceId(), null);
				String userName = userNameById.getOrDefault(r.getUser().getUserId(), null);
				Long uid = r.getUser().getUserId();
				boolean isShinhan = isShinhanByUserId.getOrDefault(uid, false);
				boolean isEmergency = emergencyEvaluator.isEmergency(r, statusName);

				return ReservationListResponseDto.from(r, statusName, spaceName, userName, isShinhan, isEmergency, previsitDtos);
			})
			.toList();

		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	// ================== 예약 상세 조회 ==================
	public ReservationDetailResponseDto getByReservationId(Long reservationId) {
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
		String status = (r.getReservationStatusId() != null)
			? r.getReservationStatusId().getReservationStatusName()
			: null;

		return new ReservationDetailResponseDto(
			r.getReservationId(),
			spaceName,
			user,
			r.getReservationPurpose(),
			r.getReservationHeadcount(),
			r.getReservationFrom(),
			r.getReservationTo(),
			r.getOrderId(),
			status
		);
	}
}
