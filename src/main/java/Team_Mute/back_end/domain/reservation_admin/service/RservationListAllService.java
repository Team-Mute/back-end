package Team_Mute.back_end.domain.reservation_admin.service;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.entity.UserCompany;
import Team_Mute.back_end.domain.member.repository.UserCompanyRepository;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation_admin.dto.response.PrevisitItemResponseDto;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminPrevisitReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationStatusRepository;
import Team_Mute.back_end.domain.reservation_admin.util.EmergencyEvaluator;
import Team_Mute.back_end.domain.reservation_admin.util.ShinhanGroupUtils;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import static Team_Mute.back_end.domain.reservation_admin.util.ReservationApprovalPolicy.isApprovableFor;
import static Team_Mute.back_end.domain.reservation_admin.util.ReservationApprovalPolicy.isRejectableFor;

@Service
public class RservationListAllService {
	private final AdminPrevisitReservationRepository adminPrevisitRepository;
	private final AdminReservationStatusRepository adminStatusRepository;
	private final SpaceRepository spaceRepository;
	private final UserRepository userRepository;
	private final UserCompanyRepository userCompanyRepository;
	private final EmergencyEvaluator emergencyEvaluator;

	public RservationListAllService(
		AdminPrevisitReservationRepository adminPrevisitRepository,
		AdminReservationStatusRepository adminStatusRepository,
		SpaceRepository spaceRepository,
		UserRepository userRepository,
		UserCompanyRepository userCompanyRepository,
		EmergencyEvaluator emergencyEvaluator
	) {
		this.adminPrevisitRepository = adminPrevisitRepository;
		this.adminStatusRepository = adminStatusRepository;
		this.spaceRepository = spaceRepository;
		this.userRepository = userRepository;
		this.userCompanyRepository = userCompanyRepository;
		this.emergencyEvaluator = emergencyEvaluator;
	}

	public List<ReservationListResponseDto> getReservationListAll(List<Reservation> reservations, Long roleId) {
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

				String statusName = statusNameById.getOrDefault(r.getReservationStatus().getReservationStatusId(), "UNKNOWN");
				String spaceName = spaceNameById.getOrDefault(r.getSpace().getSpaceId(), null);
				String userName = userNameById.getOrDefault(r.getUser().getUserId(), null);
				Long uid = r.getUser().getUserId();
				boolean isShinhan = isShinhanByUserId.getOrDefault(uid, false);
				boolean isEmergency = emergencyEvaluator.isEmergency(r, statusName);
				boolean isApprovable = isApprovableFor(roleId, statusName);
				boolean isRejectable = isRejectableFor(roleId, statusName);

				return ReservationListResponseDto.from(r, statusName, spaceName, userName, isShinhan, isEmergency, isApprovable, isRejectable, previsitDtos);
			})
			.toList();

		return content;
	}
}
