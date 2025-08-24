package Team_Mute.back_end.domain.reservation_admin.service;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.entity.UserCompany;
import Team_Mute.back_end.domain.member.repository.UserCompanyRepository;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.reservation_admin.dto.PrevisitItemResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation_admin.entity.Reservation;
import Team_Mute.back_end.domain.reservation_admin.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation_admin.repository.PrevisitReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationStatusRepository;
import Team_Mute.back_end.domain.reservation_admin.util.EmergencyEvaluator;
import Team_Mute.back_end.domain.reservation_admin.util.ShinhanGroupUtils;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationAdminService {

	private final ReservationRepository reservationRepository;
	private final PrevisitReservationRepository previsitRepository;
	private final ReservationStatusRepository statusRepository;
	private final SpaceRepository spaceRepository;
	private final UserRepository userRepository;
	private final UserCompanyRepository userCompanyRepository;
	private final EmergencyEvaluator emergencyEvaluator;

	private static final String SHINHAN = "신한금융희망재단";

	public ReservationAdminService(
		ReservationRepository reservationRepository,
		PrevisitReservationRepository previsitRepository,
		ReservationStatusRepository statusRepository,
		SpaceRepository spaceRepository,
		UserRepository userRepository,
		UserCompanyRepository userCompanyRepository,
		EmergencyEvaluator emergencyEvaluator
	) {
		this.reservationRepository = reservationRepository;
		this.previsitRepository = previsitRepository;
		this.statusRepository = statusRepository;
		this.spaceRepository = spaceRepository;
		this.userRepository = userRepository;
		this.userCompanyRepository = userCompanyRepository;
		this.emergencyEvaluator = emergencyEvaluator;
	}


	// 예약 관리 시 예약 리스트 조회
	public Page<ReservationListResponseDto> getAllReservations(Pageable pageable) {
		// 예약 페이지 로딩
		Page<Reservation> page = reservationRepository.findAll(pageable);
		List<Reservation> reservations = page.getContent();

		if (reservations.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, 0);
		}

		// 예약/사전답사에서 쓰일 상태ID 수집
		Set<Integer> statusIds = reservations.stream()
			.map(Reservation::getReservationStatusId)
			.collect(Collectors.toSet());

		// 사전답사 일괄 로딩(예약ID IN (...))
		List<Integer> reservationIds = reservations.stream()
			.map(Reservation::getReservationId)
			.toList();

		List<PrevisitReservation> previsitList = previsitRepository.findByReservation_ReservationIdIn(reservationIds);

		statusIds.addAll(previsitList.stream()
			.map(PrevisitReservation::getReservationStatusId)
			.collect(Collectors.toSet()));

		// 상태ID → 상태명 맵
		Map<Integer, String> statusNameById = statusRepository.findAllById(statusIds).stream()
			.collect(Collectors.toMap(
				ReservationStatus::getReservationStatusId,
				ReservationStatus::getReservationStatusName
			));

		// 공간/유저 이름 배치 조회
		Set<Integer> spaceId = reservations.stream().map(Reservation::getSpaceId).collect(Collectors.toSet());
		Set<Long> userId = reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());

		// spaceId -> spaceName 맵
		Map<Integer, String> spaceNameById = spaceRepository.findAllById(spaceId).stream()
			.collect(Collectors.toMap(Space::getSpaceId, Space::getSpaceName));

		// userId -> userName 맵
		Map<Long, String> userNameById = userRepository.findAllById(userId).stream()
			.collect(Collectors.toMap(User::getUserId, User::getUserName));

		// 사전답사들을 예약ID로 그룹핑
		Map<Integer, List<PrevisitReservation>> previsitMap = previsitList.stream()
			.collect(Collectors.groupingBy(p -> p.getReservation().getReservationId()));

		// 유저 목록(이름 + 연결된 회사 엔티티 LAZY) 조회
		java.util.List<User> users = userRepository.findAllById(userId);

		// 회사 id 모으기 (LAZY 지연로딩이지만 페이지당 5~6건이면 부담 적음)
		java.util.Set<Integer> companyIds = users.stream()
			.map(u -> u.getUserCompany() != null ? u.getUserCompany().getCompanyId() : null)
			.filter(java.util.Objects::nonNull)
			.collect(java.util.stream.Collectors.toSet());

		// companyId -> companyName 맵 생성 (요게 companyNameById)
		Map<Integer, String> companyNameById = userCompanyRepository.findAllById(companyIds).stream()
			.collect(Collectors.toMap(UserCompany::getCompanyId, UserCompany::getCompanyName));

		// userId -> isShinhan 맵
		Map<Long, Boolean> isShinhanByUserId =
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

				String statusName = statusNameById.getOrDefault(r.getReservationStatusId(), "UNKNOWN");
				String spaceName = spaceNameById.getOrDefault(r.getSpaceId(), null);
				String userName = userNameById.getOrDefault(r.getUserId(), null);
				Long uid = (r.getUserId() == null) ? null : Long.valueOf(r.getUserId());
				boolean isShinhan = (uid == null) ? false : isShinhanByUserId.getOrDefault(uid, false);
				boolean isEmergency = emergencyEvaluator.isEmergency(r, statusName);

				return ReservationListResponseDto.from(r, statusName, spaceName, userName, isShinhan, isEmergency, previsitDtos);
			})
			.toList();

		return new PageImpl<>(content, pageable, page.getTotalElements());
	}
}
