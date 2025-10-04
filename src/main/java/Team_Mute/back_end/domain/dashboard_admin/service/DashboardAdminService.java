package Team_Mute.back_end.domain.dashboard_admin.service;

import Team_Mute.back_end.domain.dashboard_admin.dto.ReservationCalendarResponseDto;
import Team_Mute.back_end.domain.dashboard_admin.dto.ReservationCountResponseDto;
import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ReservationListResponseDto;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.service.RservationListAllService;
import Team_Mute.back_end.global.constants.ReservationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드 화면에 필요한 데이터를 처리하는 서비스 클래스
 * 예약 현황 카운트, 캘린더 목록, 특정 날짜의 예약 목록 조회 로직을 포함
 * 모든 메서드는 관리자 권한을 확인하는 로직을 포함
 *
 * @author Team Mute
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class DashboardAdminService {
	private final AdminReservationRepository adminReservationRepository;
	private final AdminRepository adminRepository;
	private final RservationListAllService rservationListAllService;

	/**
	 * DashboardAdminService의 생성자
	 *
	 * @param adminReservationRepository 예약 엔티티에 접근하는 레포지토리
	 * @param adminRepository            관리자 엔티티에 접근하는 레포지토리
	 * @param rservationListAllService   전체 예약 목록 변환 로직을 담당하는 서비스
	 */
	public DashboardAdminService(
		AdminReservationRepository adminReservationRepository,
		AdminRepository adminRepository,
		RservationListAllService rservationListAllService
	) {
		this.adminReservationRepository = adminReservationRepository;
		this.adminRepository = adminRepository;
		this.rservationListAllService = rservationListAllService;
	}

	/**
	 * 대시보드 카드
	 * - 관리자 대시보드에 표시할 예약 현황 카운트 정보를 계산하여 반환
	 * - 관리자 ID를 기반으로 권한 확인 후, 전체 예약 데이터를 필터링하여 각 카테고리별 건수를 집계
	 *
	 * @param adminId 현재 로그인한 관리자의 ID
	 * @return 각 예약 카테고리별 건수를 담고 있는 {@code ReservationCountResponseDto}
	 */
	public ReservationCountResponseDto getReservationCounts(Long adminId) {
		// 관리자 유효성 검사
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(Team_Mute.back_end.domain.member.exception.UserNotFoundException::new);

		// DB에서 모든 예약 데이터를 가져옴
		List<Reservation> allReservations = adminReservationRepository.findAll();

		// 1차 승인자 필터링 로직을 포함한 전체 리스트 변환 (관리자 권한 필터링 적용)
		List<ReservationListResponseDto> allContent = rservationListAllService.getReservationListAll(allReservations, admin);

		// 1차 승인 대기 건수 집계
		long waitingFistApprovalCount = allContent.stream()
			.filter(dto -> dto.getStatusId().equals(ReservationStatus.WAITING_FIRST_APPROVAL))
			.count();

		// 2차 승인 대기 건수 집계
		long waitingSecondApprovalCount = allContent.stream()
			.filter(dto -> dto.getStatusId().equals(ReservationStatus.WAITING_SECOND_APPROVAL))
			.count();

		// 긴급 예약 건수 집계
		long emergencyCount = allContent.stream()
			.filter(dto -> Boolean.TRUE.equals(dto.isEmergency))
			.count();

		// 신한 관련 예약 건수 집계
		long shinhanCount = allContent.stream()
			.filter(dto -> Boolean.TRUE.equals(dto.isShinhan))
			.count();

		// DTO에 담아서 반환
		return new ReservationCountResponseDto(
			waitingFistApprovalCount,
			waitingSecondApprovalCount,
			emergencyCount,
			shinhanCount
		);
	}

	/**
	 * 대시보드 캘린더 리스트 조회
	 * - 관리자 대시보드 캘린더에 표시할 전체 예약 리스트를 조회
	 * - 1차 승인 대기, 2차 승인 대기, 최종 승인 완료, 이용 완료 상태의 예약만 추출하여 캘린더 형식 DTO로 변환
	 *
	 * @param adminId 현재 로그인한 관리자의 ID
	 * @return 캘린더 표시에 필요한 핵심 정보만 담은 예약 리스트 DTO 목록
	 */
	@Transactional(readOnly = true)
	public List<ReservationCalendarResponseDto> getAllReservations(Long adminId) {
		// 관리자 유효성 검사
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(Team_Mute.back_end.domain.member.exception.UserNotFoundException::new);

		// DB에서 모든 예약 데이터를 가져옴
		List<Reservation> allReservations = adminReservationRepository.findAll();

		// 1차 승인자 필터링 로직을 포함한 전체 리스트 변환 (관리자 권한 필터링 적용)
		List<ReservationListResponseDto> allContent = rservationListAllService.getReservationListAll(allReservations, admin);

		// 1차 승인 대기, 2차 승인 대기, 최종 승인 완료, 이용 완료인 리스트만 추출 후 새로운 DTO로 변환
		List<ReservationCalendarResponseDto> responseDtos = allContent.stream()
			.filter(dto -> dto.getStatusId().equals(ReservationStatus.WAITING_FIRST_APPROVAL) || dto.getStatusId().equals(ReservationStatus.WAITING_SECOND_APPROVAL) || dto.getStatusId().equals(ReservationStatus.FINAL_APPROVAL) || dto.getStatusId().equals(ReservationStatus.USER_COMPLETED))
			.map(ReservationCalendarResponseDto::from)
			.collect(Collectors.toList());

		return responseDtos;
	}

	/**
	 * 대시보드 특정 날짜 예약 리스트 조회
	 * - 특정 날짜에 해당하는 상세 예약 리스트를 조회
	 * - 예약의 시작일 또는 종료일이 주어진 날짜와 일치하는 예약을 필터링하여 반환
	 *
	 * @param adminId 현재 로그인한 관리자의 ID
	 * @param date    조회할 날짜 ({@code LocalDate} 형식)
	 * @return 특정 날짜에 해당하는 상세 예약 리스트 DTO 목록
	 */
	@Transactional(readOnly = true)
	public List<ReservationListResponseDto> getReservationsByDate(Long adminId, LocalDate date) {
		// 관리자 유효성 검사
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(Team_Mute.back_end.domain.member.exception.UserNotFoundException::new);

		// DB에서 모든 예약 데이터를 가져옴
		List<Reservation> allReservations = adminReservationRepository.findAll();

		// 1차 승인자 필터링 로직을 포함한 전체 리스트 변환 (관리자 권한 필터링 적용)
		List<ReservationListResponseDto> allContent = rservationListAllService.getReservationListAll(allReservations, admin);

		// 특정 날짜(예약 시작일 또는 종료일)에 해당하는 리스트만 추출
		List<ReservationListResponseDto> responseDtos = allContent.stream()
			.filter(dto -> dto.getReservationFrom().toLocalDate().isEqual(date) || dto.getReservationTo().toLocalDate().isEqual(date))
			.toList();

		return responseDtos;
	}
}
