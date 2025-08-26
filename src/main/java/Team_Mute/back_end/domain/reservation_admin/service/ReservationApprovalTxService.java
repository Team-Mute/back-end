package Team_Mute.back_end.domain.reservation_admin.service;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation_admin.dto.response.ApproveResponseDto;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminPrevisitReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationRepository;
import Team_Mute.back_end.domain.reservation_admin.repository.AdminReservationStatusRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor // (로봇이 Lombok 사용 중이라면 유지, 아니라면 생성자 직접 작성)
public class ReservationApprovalTxService {

	private final AdminRepository adminRepository;
	private final AdminReservationRepository adminReservationRepository;
	private final AdminReservationStatusRepository adminStatusRepository;
	private final AdminPrevisitReservationRepository adminPrevisitRepository;

	private static final String STATUS_FIRST_PENDING = "1차 승인 대기";
	private static final String STATUS_SECOND_PENDING = "2차 승인 대기";
	private static final String STATUS_FINAL_APPROVED = "최종 승인 완료";
	private static final Long ROLE_SECOND_APPROVER = 1L; // [ADD] 2차 승인자(1,2차 가능)
	private static final Long ROLE_FIRST_APPROVER = 2L; // [ADD] 1차 승인자(1차만 가능)

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

	// ================== 1차 승인 (Tx 보장) ==================
	@Transactional // [ADD] 각 건 단위 트랜잭션
	public ApproveResponseDto approveFirstTx(Long adminId, Long reservationId) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));

		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());

		// findById 그대로 사용해도, 지금은 트랜잭션/세션 안이라 LAZY 접근 안전
		Reservation r = adminReservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

		Long fromStatusId = r.getReservationStatusId().getReservationStatusId();
		String fromStatus = adminStatusRepository.findById(fromStatusId)
			.map(ReservationStatus::getReservationStatusName).orElse("UNKNOWN");

		if (!STATUS_FIRST_PENDING.equals(fromStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"1차 승인 불가(이미 처리 완료된 대상인지 확인하세요): " + fromStatus);
		}

		if (ROLE_FIRST_APPROVER.equals(roleId) || ROLE_SECOND_APPROVER.equals(roleId)) {
			ReservationStatus toStatus = adminStatusRepository.findById(statusId(STATUS_SECOND_PENDING))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found"));

			// 예약 상태 변경
			r.setReservationStatusId(toStatus);
			r.setUpdDate(LocalDateTime.now());

			// 사전답사 상태 동기화 (LAZY라도 지금은 세션 O)
			if (r.getPrevisitReservations() != null) {
				for (PrevisitReservation previsit : r.getPrevisitReservations()) {
					previsit.setReservationStatusId(toStatus.getReservationStatusId());
					previsit.setUpdDate(LocalDateTime.now());
				}
				adminPrevisitRepository.saveAll(r.getPrevisitReservations());
			}

			return new ApproveResponseDto(
				reservationId, fromStatus, STATUS_SECOND_PENDING, LocalDateTime.now(), "1차 승인 완료"
			);
		} else {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인 권한이 없습니다.");
		}
	}

	// ================== 2차 승인 (Tx 보장) ==================
	@Transactional // [ADD]
	public ApproveResponseDto approveSecondTx(Long adminId, Long reservationId) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));

		Long roleId = Long.valueOf(admin.getUserRole().getRoleId());

		Reservation r = adminReservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

		Long fromStatusId = r.getReservationStatusId().getReservationStatusId();
		String fromStatus = adminStatusRepository.findById(fromStatusId)
			.map(ReservationStatus::getReservationStatusName).orElse("UNKNOWN");

		// 허용 전이: FIRST_PENDING or SECOND_PENDING -> FINAL_APPROVED
		if (!STATUS_FIRST_PENDING.equals(fromStatus) && !STATUS_SECOND_PENDING.equals(fromStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"2차 승인 불가(이미 처리 완료된 대상인지 확인하세요): " + fromStatus);
		}

		if (ROLE_SECOND_APPROVER.equals(roleId)) {
			ReservationStatus toStatus = adminStatusRepository.findById(statusId(STATUS_FINAL_APPROVED))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found"));

			r.setReservationStatusId(toStatus);
			r.setUpdDate(LocalDateTime.now());

			if (r.getPrevisitReservations() != null) {
				for (PrevisitReservation previsit : r.getPrevisitReservations()) {
					previsit.setReservationStatusId(toStatus.getReservationStatusId());
					previsit.setUpdDate(LocalDateTime.now());
				}
				adminPrevisitRepository.saveAll(r.getPrevisitReservations());
			}

			return new ApproveResponseDto(
				reservationId, fromStatus, STATUS_FINAL_APPROVED, LocalDateTime.now(), "2차 승인 완료"
			);
		} else {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인 권한이 없습니다.");
		}
	}
}
