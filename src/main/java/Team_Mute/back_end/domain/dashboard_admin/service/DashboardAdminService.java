package Team_Mute.back_end.domain.dashboard_admin.service;

import Team_Mute.back_end.domain.dashboard_admin.dto.ReservationCountResponseDto;
import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.service.RservationListAllService;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardAdminService {
	private final AdminReservationRepository adminReservationRepository;
	private final AdminRepository adminRepository;
	private final RservationListAllService rservationListAllService;

	private static final Long WAITING_FIRST_APPROVAL = 1L; // 1차 승인 대기
	private static final Long WAITING_SECOND_APPROVAL = 2L; // 2차 승인 대기
	private static final Long FINAL_APPROVAL = 3L; // 최종 승인 완료
	private static final Long USER_COMPLETED = 5L; // 이용 완료

	public DashboardAdminService(
		AdminReservationRepository adminReservationRepository,
		AdminRepository adminRepository,
		RservationListAllService rservationListAllService
	) {
		this.adminReservationRepository = adminReservationRepository;
		this.adminRepository = adminRepository;
		this.rservationListAllService = rservationListAllService;
	}

	// 대시보드 카드
	public ReservationCountResponseDto getReservationCounts(Long adminId) {
		// 1. 전체 데이터 로드
		// 이전에 말씀드린 대로, DB에 있는 모든 데이터를 메모리로 가져옵니다.
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(Team_Mute.back_end.domain.member.exception.UserNotFoundException::new);

		// DB에서 모든 예약 데이터를 가져옴
		List<Reservation> allReservations = adminReservationRepository.findAll();

		// 1차 승인자 필터링 로직을 포함한 전체 리스트 변환
		List<ReservationListResponseDto> allContent = rservationListAllService.getReservationListAll(allReservations, admin);

		// 1차 승인 대기
		long waitingFistApprovalCount = allContent.stream()
			.filter(dto -> dto.getStatusId().equals(WAITING_FIRST_APPROVAL))
			.count();

		long waitingSecondApprovalCount = allContent.stream()
			.filter(dto -> dto.getStatusId().equals(WAITING_SECOND_APPROVAL))
			.count();

		long emergencyCount = allContent.stream()
			.filter(dto -> Boolean.TRUE.equals(dto.isEmergency))
			.count();

		long shinhanCount = allContent.stream()
			.filter(dto -> Boolean.TRUE.equals(dto.isShinhan))
			.count();

		// 4. DTO에 담아서 반환
		return new ReservationCountResponseDto(
			waitingFistApprovalCount,
			waitingSecondApprovalCount,
			emergencyCount,
			shinhanCount
		);
	}

	// ================== 대시보드 캘린더 리스트 조회 ==================
	@Transactional(readOnly = true)
	public List<ReservationListResponseDto> getAllReservations(Long adminId) {
		// 관리자 권한
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(Team_Mute.back_end.domain.member.exception.UserNotFoundException::new);

		// DB에서 모든 예약 데이터를 가져옴
		List<Reservation> allReservations = adminReservationRepository.findAll();

		// 1차 승인자 필터링 로직을 포함한 전체 리스트 변환
		List<ReservationListResponseDto> allContent = rservationListAllService.getReservationListAll(allReservations, admin);

		// 1차 승인 대기, 2차 승인 대기, 최종 승인 완료, 이용 완료인 리스트만 추출
		List<ReservationListResponseDto> responseDtos = allContent.stream()
			.filter(dto -> dto.getStatusId().equals(WAITING_FIRST_APPROVAL) || dto.getStatusId().equals(WAITING_SECOND_APPROVAL) || dto.getStatusId().equals(FINAL_APPROVAL) || dto.getStatusId().equals(USER_COMPLETED))
			.toList();

		return responseDtos;
	}
}
