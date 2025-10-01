package Team_Mute.back_end.domain.reservation_admin.service;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.entity.UserCompany;
import Team_Mute.back_end.domain.member.repository.UserCompanyRepository;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminPrevisitReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationStatusRepository;
import Team_Mute.back_end.domain.reservation_admin.util.EmergencyEvaluator;
import Team_Mute.back_end.domain.reservation_admin.util.ShinhanGroupUtils;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;

import java.util.Comparator;
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

	private static final Integer ROLE_SECOND_APPROVER = 1; // 2차 승인자(1,2차 가능)
	private static final Integer ROLE_FIRST_APPROVER = 2; // 1차 승인자(1차만 가능)

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

	public List<ReservationListResponseDto> getReservationListAll(List<Reservation> reservations, Admin admin) {
		Integer adminRole = admin.getUserRole().getRoleId(); // 관리자의 권한 ID
		// 리스트 정렬: 관리자의 역할에 따라 정렬 기준을 동적으로 설정
		reservations = reservations.stream()
			.sorted(
				// 1차 정렬: getStatusOrder 메서드를 통해 상태 우선순위 결정
				Comparator.comparing(
						// 람다식의 인자에 타입을 명시
						(Reservation reservation) -> getStatusOrder(reservation.getReservationStatus().getReservationStatusId(), admin.getUserRole().getRoleId())
					)
					// 2차 정렬: 1차 정렬 결과가 같을 경우, 등록일(getRegDate)을 내림차순(최신순)으로 정렬
					.thenComparing(
						Reservation::getRegDate, Comparator.reverseOrder()
					)
			)
			.collect(Collectors.toList());

		// 관리자 담당 지역 ID 가져오기
		Integer adminRegionId;
		if (admin.getAdminRegion() != null) {
			adminRegionId = admin.getAdminRegion().getRegionId();
		} else {
			adminRegionId = null;
		}

		// 1차 승인자일 경우 리스트를 관리 지역만 필터링
		if (adminRole.equals(ROLE_FIRST_APPROVER) && adminRegionId != null) {
			reservations = reservations.stream()
				.filter(r -> {
					Integer spaceRegionId = r.getSpace().getRegionId(); // 예약된 공간의 지역 ID
					boolean isMatch = spaceRegionId.equals(adminRegionId);
					return isMatch;
				})
				.collect(Collectors.toList());
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

		// 버튼 클릭 활성화를 위한 권한 체크
		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());


		// DTO 변환
		List<ReservationListResponseDto> content = reservations.stream()
			.map(reservation -> {
				// 버튼 클릭 활성화를 위한 권한 체크
				Integer reservationRegionId = reservation.getSpace().getRegionId(); // 예약된 공간의 지역ID 조회

				String statusName = statusNameById.getOrDefault(reservation.getReservationStatus().getReservationStatusId(), "UNKNOWN");
				String spaceName = spaceNameById.getOrDefault(reservation.getSpace().getSpaceId(), null);
				String userName = userNameById.getOrDefault(reservation.getUser().getUserId(), null);
				Long uid = reservation.getUser().getUserId();
				boolean isShinhan = isShinhanByUserId.getOrDefault(uid, false);
				boolean isEmergency = emergencyEvaluator.isEmergency(reservation, statusName);
				boolean isApprovable = isApprovableFor(reservationRegionId, adminRegionId, roleId, statusName);
				boolean isRejectable = isRejectableFor(reservationRegionId, adminRegionId, roleId, statusName);

				return ReservationListResponseDto.from(reservation, statusName, spaceName, userName, isShinhan, isEmergency, isApprovable, isRejectable);
			})
			.toList();

		return content;
	}

	private int getStatusOrder(Long statusId, Integer adminRole) {
		if (adminRole.equals(ROLE_SECOND_APPROVER)) {
			return switch (statusId.intValue()) {
				case 2 -> 1;
				case 1 -> 2;
				case 3 -> 3;
				case 4 -> 4;
				case 5 -> 5;
				case 6 -> 6;
				default -> 99;
			};
		} else {
			return statusId.intValue();
		}
	}
}
