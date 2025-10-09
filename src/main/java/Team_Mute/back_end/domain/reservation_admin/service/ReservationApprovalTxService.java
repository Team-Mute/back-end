package Team_Mute.back_end.domain.reservation_admin.service;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ApproveResponseDto;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationStatusRepository;
import Team_Mute.back_end.global.constants.AdminRoleEnum;
import Team_Mute.back_end.global.constants.ReservationStatusEnum;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * [예약 승인 트랜잭션] 전용 서비스
 * - 예약의 1차 승인 및 2차 승인 로직을 각각 독립된 @Transactional 단위로 처리하여 데이터 변경의 원자성(Atomicity)과 일관성을 보장
 */
@Service
@RequiredArgsConstructor
public class ReservationApprovalTxService {

	private final AdminRepository adminRepository;
	private final AdminReservationRepository adminReservationRepository;
	private final AdminReservationStatusRepository adminStatusRepository;

	// 상태명(Description) -> 상태ID(Long) 캐시: DB 반복 조회를 줄이기 위한 간단한 메모리 캐시
	private final Map<String, Long> statusIdCache = new HashMap<>();

	/**
	 * 상태명(String)을 기준으로 DB에서 상태 엔티티 ID(Long)를 조회하거나 캐시에서 가져옴
	 *
	 * @param name ReservationStatusEnum.getDescription() (예: "1차 승인 대기")
	 * @return ReservationStatusId (Long) (예: 1)
	 * @throws ResponseStatusException 상태를 찾을 수 없을 때
	 */
	private Long statusId(String name) {
		return statusIdCache.computeIfAbsent(name, key ->
			adminStatusRepository.findByReservationStatusName(key)
				.map(ReservationStatus::getReservationStatusId)
				.orElseThrow(() -> new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Unknown reservation status: " + key))
		);
	}

	/**
	 * 1차 승인 로직을 트랜잭션 단위로 실행
	 * - 1차 승인자: WAITING_FIRST_APPROVAL(1차 승인 대기) 상태의 예약을 WAITING_SECOND_APPROVAL(2차 승인 대기)로 변경
	 *
	 * @param adminId       승인을 요청한 관리자 ID (Long)
	 * @param reservationId 승인 대상 예약 ID (Long)
	 * @return 승인 결과 DTO
	 * @throws ResponseStatusException 권한, 상태 불일치, 지역 불일치 등 오류 발생 시
	 */
	@Transactional // 각 건 단위 트랜잭션
	public ApproveResponseDto approveFirstTx(Long adminId, Long reservationId) {
		// 관리자 유효성 및 권한 ID 조회
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));

		Integer roleId = admin.getUserRole().getRoleId();

		// 예약 엔티티 조회
		Reservation reservation = adminReservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

		Integer reservationRegionId = reservation.getSpace().getRegionId(); // 예약된 공간의 지역ID 조회

		// 현재 예약 상태 확인
		Long fromStatusId = reservation.getReservationStatusId().getReservationStatusId();
		String fromStatus = adminStatusRepository.findById(fromStatusId)
			.map(ReservationStatus::getReservationStatusName).orElse("UNKNOWN");

		// 1차 승인 대기 상태인지 확인 (상태 전이 유효성 검사)
		if (!ReservationStatusEnum.WAITING_FIRST_APPROVAL.getDescription().equals(fromStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"1차 승인 불가(이미 처리 완료된 대상인지 확인하세요): " + fromStatus);
		}

		// 권한 및 지역 검사
		if (AdminRoleEnum.ROLE_FIRST_APPROVER.getId().equals(roleId) || AdminRoleEnum.ROLE_SECOND_APPROVER.getId().equals(roleId)) {
			// 1차 승인자일 경우 담당 지역만 승인 가능
			if (AdminRoleEnum.ROLE_FIRST_APPROVER.getId().equals(roleId)) {
				Integer adminRegionId = admin.getAdminRegion().getRegionId(); // 관리자의 담당 지역 ID
				if (!reservationRegionId.equals(adminRegionId)) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"해당 지역의 승인 권한이 없습니다 담당 지역인지 확인하세요");
				}
			}

			// 상태 변경 (WAITING_SECOND_APPROVAL(2차 승인 대기)로 전이)
			ReservationStatus toStatus = adminStatusRepository.findById(statusId(ReservationStatusEnum.WAITING_SECOND_APPROVAL.getDescription()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found"));

			// 예약 상태 변경
			reservation.setReservationStatusId(toStatus);
			reservation.setUpdDate(LocalDateTime.now());

			return new ApproveResponseDto(
				reservationId, fromStatus, ReservationStatusEnum.WAITING_SECOND_APPROVAL.getDescription(), LocalDateTime.now(), "1차 승인 완료"
			);
		} else {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인 권한이 없습니다.");
		}
	}

	/**
	 * 2차 승인 로직을 트랜잭션 단위로 실행
	 * 2차 승인자가 실행
	 * - 1차 승인 대기 상태일 경우: WAITING_FIRST_APPROVAL(1차 승인 대기) 상태의 예약을 FINAL_APPROVAL(최종 승인 완료)로 변경
	 * - 2차 승인 대기 상태일 경우: WAITING_SECOND_APPROVAL(2차 승인 대기) 상태의 예약을 FINAL_APPROVAL(최종 승인 완료)로 변경
	 *
	 * @param adminId       승인을 요청한 관리자 ID (Long)
	 * @param reservationId 승인 대상 예약 ID (Long)
	 * @return 승인 결과 DTO
	 * @throws ResponseStatusException 권한, 상태 불일치 등 오류 발생 시
	 */
	@Transactional
	public ApproveResponseDto approveSecondTx(Long adminId, Long reservationId) {
		// 관리자 유효성 및 권한 ID 조회
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));

		Integer roleId = admin.getUserRole().getRoleId();

		// 예약 엔티티 조회
		Reservation reservation = adminReservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

		// 현재 예약 상태 확인
		Long fromStatusId = reservation.getReservationStatusId().getReservationStatusId();
		String fromStatus = adminStatusRepository.findById(fromStatusId)
			.map(ReservationStatus::getReservationStatusName).orElse("UNKNOWN");

		// 2차 승인 가능 상태인지 확인
		// 허용 전이: WAITING_FIRST_APPROVAL과 or WAITING_SECOND_APPROVAL과 -> FINAL_APPROVED
		if (!ReservationStatusEnum.WAITING_FIRST_APPROVAL.getDescription().equals(fromStatus) && !ReservationStatusEnum.WAITING_SECOND_APPROVAL.getDescription().equals(fromStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"2차 승인 불가(이미 처리 완료된 대상인지 확인하세요): " + fromStatus);
		}
		// 권한 검사
		if (AdminRoleEnum.ROLE_SECOND_APPROVER.getId().equals(roleId)) {
			// 상태 변경 (FINAL_APPROVAL(최종 승인 완료)로 전이)
			ReservationStatus toStatus = adminStatusRepository.findById(statusId(ReservationStatusEnum.FINAL_APPROVAL.getDescription()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found"));

			reservation.setReservationStatusId(toStatus);
			reservation.setUpdDate(LocalDateTime.now());

			return new ApproveResponseDto(
				reservationId, fromStatus, ReservationStatusEnum.FINAL_APPROVAL.getDescription(), LocalDateTime.now(), "2차 승인 완료"
			);
		} else {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인 권한이 없습니다.");
		}
	}
}
