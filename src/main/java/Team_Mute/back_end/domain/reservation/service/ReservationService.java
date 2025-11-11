package Team_Mute.back_end.domain.reservation.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.reservation.dto.request.ReservationRequestDto;
import Team_Mute.back_end.domain.reservation.dto.response.PagedReservationResponse;
import Team_Mute.back_end.domain.reservation.dto.response.RejectReasonResponseDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationCancelResponseDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationDetailResponseDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationListDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationResponseDto;
import Team_Mute.back_end.domain.reservation.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation.exception.ForbiddenAccessException;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.exception.ReservationConflictException;
import Team_Mute.back_end.domain.reservation.exception.ResourceNotFoundException;
import Team_Mute.back_end.domain.reservation.repository.PrevisitRepository;
import Team_Mute.back_end.domain.reservation.repository.ReservationRepository;
import Team_Mute.back_end.domain.reservation.repository.ReservationStatusRepository;
import Team_Mute.back_end.domain.reservation_admin.entity.ReservationLog;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationLogRepository;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import Team_Mute.back_end.domain.space_admin.util.S3Deleter;
import Team_Mute.back_end.domain.space_admin.util.S3Uploader;
import lombok.RequiredArgsConstructor;

/**
 * 예약 비즈니스 로직 서비스
 * 예약 CRUD, 중복 검증, 파일 업로드, 상태 관리 기능 제공
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final SpaceRepository spaceRepository;
	private final ReservationStatusRepository reservationStatusRepository;
	private final UserRepository userRepository;
	private final S3Uploader s3Uploader;
	private final S3Deleter s3Deleter;
	private final ReservationLogRepository reservationLogRepository;
	private final PrevisitRepository previsitReservationRepository;

	/**
	 * 예약 생성
	 *
	 * 처리 흐름:
	 * 1. 사용자 권한 확인 (roleId=3)
	 * 2. 비관적 락으로 중복 예약 검증 (일반 + 사전답사)
	 * 3. 예약 생성 및 저장 (초기 상태: 승인 대기)
	 * 4. 첨부 파일 S3 업로드
	 * 5. 사전답사 예약 생성 (선택적)
	 *
	 * 동시성 제어:
	 * - PESSIMISTIC_WRITE 락으로 중복 예약 방지
	 *
	 * @param userId 사용자 ID
	 * @param requestDto 예약 요청 DTO
	 * @return 생성된 예약 DTO
	 * @throws ForbiddenAccessException 권한 없음
	 * @throws ReservationConflictException 중복 예약
	 */
	@Transactional
	public ReservationResponseDto createReservation(String userId, ReservationRequestDto requestDto) {
		User user = findUserById(userId);

		// 1. 일반 사용자만 예약 생성 가능
		if (user.getUserRole().getRoleId() != 3) {
			throw new ForbiddenAccessException("예약을 생성할 권한이 없습니다.");
		}

		final List<Long> validStatusIds = Arrays.asList(1L, 2L, 3L);

		Space space = spaceRepository.findById(requestDto.getSpaceId())
			.orElseThrow(() -> new ResourceNotFoundException("해당 공간을 찾을 수 없습니다."));

		// 2. 비관적 락으로 중복 예약 검증 (공간 예약)
		List<Reservation> overlappingReservations = reservationRepository.findOverlappingReservationsWithLock(
			space.getSpaceId(),
			requestDto.getReservationFrom(),
			requestDto.getReservationTo(),
			validStatusIds
		);

		// 3. 비관적 락으로 중복 검증 (사전답사)
		List<PrevisitReservation> overlappingPrevisits = previsitReservationRepository.findOverlappingPrevisitsWithLock(
			space.getSpaceId(),
			requestDto.getReservationFrom(),
			requestDto.getReservationTo(),
			validStatusIds
		);

		if (!overlappingReservations.isEmpty() || !overlappingPrevisits.isEmpty()) {
			throw new ReservationConflictException("해당 시간에는 확정된 예약 또는 사전 답사가 존재하여 예약할 수 없습니다.");
		}

		// 4. 예약 객체 생성 및 저장 (초기 상태: 승인 대기)
		final Long INITIAL_RESERVATION_STATUS_ID = 1L;
		ReservationStatus status = reservationStatusRepository.findById(INITIAL_RESERVATION_STATUS_ID)
			.orElseThrow(
				() -> new ResourceNotFoundException("기본 예약 상태(ID: " + INITIAL_RESERVATION_STATUS_ID + ")를 찾을 수 없습니다."));

		Reservation reservation = Reservation.builder()
			.orderId(generateOrderId(space.getSpaceName()))
			.space(space)
			.user(user)
			.reservationStatus(status)
			.reservationHeadcount(requestDto.getReservationHeadcount())
			.reservationFrom(requestDto.getReservationFrom())
			.reservationTo(requestDto.getReservationTo())
			.reservationPurpose(requestDto.getReservationPurpose())
			.reservationAttachment(new ArrayList<>())
			.build();

		Reservation savedReservation = reservationRepository.save(reservation);

		// 5. 첨부 파일 S3 업로드
		List<String> attachmentUrls = new ArrayList<>();
		if (requestDto.getReservationAttachments() != null && !requestDto.getReservationAttachments().isEmpty()) {
			String dirName = "attachment/" + savedReservation.getReservationId();
			attachmentUrls = s3Uploader.uploadAll(requestDto.getReservationAttachments(), dirName);
		}

		savedReservation.setReservationAttachment(attachmentUrls);
		Reservation finalReservation = reservationRepository.save(savedReservation);

		// 6. 사전답사 예약 생성 (선택적)
		if (requestDto.getPrevisitInfo() != null) {
			var pReq = requestDto.getPrevisitInfo();

			// 사전답사 시간 유효성 검증
			if (pReq.getPrevisitTo().isAfter(requestDto.getReservationFrom())) {
				throw new InvalidInputValueException("사전답사 종료 시간은 공간 예약 시작 시간 이전이어야 합니다.");
			}

			if (pReq.getPrevisitFrom().isAfter(pReq.getPrevisitTo()) || pReq.getPrevisitFrom()
				.isEqual(pReq.getPrevisitTo())) {
				throw new InvalidInputValueException("사전답사 시작 시간은 종료 시간보다 이전이어야 합니다.");
			}

			// 사전답사 시간대 중복 검증
			List<Reservation> overlappingReservationsForPrevisit = reservationRepository.findOverlappingReservationsWithLock(
				space.getSpaceId(),
				pReq.getPrevisitFrom(),
				pReq.getPrevisitTo(),
				validStatusIds
			);

			List<PrevisitReservation> overlappingPrevisitsForPrevisit = previsitReservationRepository.findOverlappingPrevisitsWithLock(
				space.getSpaceId(),
				pReq.getPrevisitFrom(),
				pReq.getPrevisitTo(),
				validStatusIds
			);

			if (!overlappingReservationsForPrevisit.isEmpty() || !overlappingPrevisitsForPrevisit.isEmpty()) {
				throw new ReservationConflictException("해당 시간에는 확정된 예약 또는 사전 답사가 존재하여 예약할 수 없습니다.");
			}

			PrevisitReservation previsit = new PrevisitReservation();
			previsit.setReservation(finalReservation);
			previsit.setPrevisitFrom(pReq.getPrevisitFrom());
			previsit.setPrevisitTo(pReq.getPrevisitTo());

			PrevisitReservation savedPrevisit = previsitReservationRepository.save(previsit);
			finalReservation.setPrevisitReservation(savedPrevisit);
		}

		return ReservationResponseDto.fromEntity(finalReservation);
	}

	/**
	 * 예약 목록 조회 (페이징 및 필터링)
	 *
	 * @param userId 사용자 ID
	 * @param filterOption 필터 옵션 ("진행중", "예약완료", "이용완료", "취소", null)
	 * @param pageable 페이징 정보 (null이면 unpaged)
	 * @return 페이징된 예약 목록
	 * @throws ForbiddenAccessException 일반 사용자 아님
	 */
	@Transactional(readOnly = true)
	public PagedReservationResponse findReservations(String userId, String filterOption, Pageable pageable) {
		User user = findUserById(userId);

		if (user.getUserRole().getRoleId() != 3) {
			throw new ForbiddenAccessException("일반 사용자만 접근 가능한 기능입니다.");
		}

		Pageable pageableToUse = (pageable != null) ? pageable : Pageable.unpaged();

		// QueryDSL 동적 쿼리 실행
		Page<Reservation> reservationPage = reservationRepository.findReservationsByFilter(user, filterOption,
			pageableToUse);

		Page<ReservationListDto> dtoPage = reservationPage.map(ReservationListDto::fromEntity);

		return PagedReservationResponse.fromPage(dtoPage);
	}

	/**
	 * 예약 상세 조회
	 *
	 * @param userId 사용자 ID
	 * @param reservationId 예약 ID
	 * @return 예약 상세 DTO
	 * @throws ForbiddenAccessException 권한 없음
	 */
	@Transactional(readOnly = true)
	public ReservationDetailResponseDto findReservationById(String userId, Long reservationId) {
		User user = findUserById(userId);

		if (user.getUserRole().getRoleId() != 3) {
			throw new ForbiddenAccessException("일반 사용자만 접근 가능한 기능입니다.");
		}

		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		return ReservationDetailResponseDto.fromEntity(reservation);
	}

	/**
	 * 예약 삭제 (Hard Delete)
	 * S3 첨부 파일도 함께 삭제
	 *
	 * @param userId 사용자 ID
	 * @param reservationId 예약 ID
	 */
	public void deleteReservation(String userId, Long reservationId) {
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		// S3 첨부 파일 삭제
		List<String> attachmentUrls = reservation.getReservationAttachment();
		if (attachmentUrls != null && !attachmentUrls.isEmpty()) {
			attachmentUrls.forEach(s3Deleter::deleteByUrl);
		}

		reservationRepository.delete(reservation);
	}

	/**
	 * 예약 취소 (상태 변경: 취소됨)
	 *
	 * 취소 가능 상태: 1,2,3,4 (1차, 2차 승인 대기, 최종 승인, 반려)
	 *
	 * @param userId 사용자 ID
	 * @param reservationId 예약 ID
	 * @return 취소 결과 DTO
	 * @throws IllegalArgumentException 취소 불가능한 상태
	 */
	public ReservationCancelResponseDto cancelReservation(String userId, Long reservationId) {
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		// 취소 가능 상태 확인
		ReservationStatus currentStatus = reservation.getReservationStatus();
		Integer currentStatusId = currentStatus.getReservationStatusId();
		List<Integer> cancellableStatusIds = Arrays.asList(1, 2, 3, 4);

		if (!cancellableStatusIds.contains(currentStatusId)) {
			throw new IllegalArgumentException("이미 취소되었거나 이용 완료된 예약은 취소할 수 없습니다.");
		}

		// 상태를 '취소됨'(6)으로 변경
		final Long CANCELLED_STATUS_ID = 6L;
		ReservationStatus cancelledStatus = reservationStatusRepository.findById(CANCELLED_STATUS_ID)
			.orElseThrow(
				() -> new ResourceNotFoundException("예약 상태 '취소됨'(ID: " + CANCELLED_STATUS_ID + ")을 찾을 수 없습니다."));

		reservation.setReservationStatusId(cancelledStatus);
		reservationRepository.save(reservation);

		return ReservationCancelResponseDto.builder()
			.reservationId(reservation.getReservationId())
			.fromStatus(currentStatus.getReservationStatusName())
			.toStatus(cancelledStatus.getReservationStatusName())
			.approvedAt(ZonedDateTime.now())
			.message("예약 상태 변경 성공")
			.build();
	}

	/**
	 * 반려 사유 조회
	 * 반려 상태(4)의 예약에 대한 관리자 메모 조회
	 *
	 * @param userId 사용자 ID
	 * @param reservationId 예약 ID
	 * @return 반려 사유 DTO
	 * @throws IllegalArgumentException 반려 상태가 아님
	 */
	@Transactional(readOnly = true)
	public RejectReasonResponseDto findRejectReason(String userId, Long reservationId) {
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		final Long REJECTED_STATUS_ID = 4L;
		if (!reservation.getReservationStatus().getReservationStatusId().equals(REJECTED_STATUS_ID)) {
			throw new IllegalArgumentException("반려 상태의 예약이 아닙니다.");
		}

		ReservationLog rejectLog = reservationLogRepository
			.findTopByReservationReservationIdAndChangedStatusReservationStatusIdOrderByRegDateDesc(
				reservationId, REJECTED_STATUS_ID)
			.orElseThrow(() -> new ResourceNotFoundException("반려 사유를 찾을 수 없습니다."));

		return new RejectReasonResponseDto(rejectLog.getMemo());
	}

	/**
	 * 사용자 ID로 사용자 조회
	 */
	private User findUserById(String userId) {
		try {
			Long parsedUserId = Long.parseLong(userId);
			return userRepository.findById(parsedUserId)
				.orElseThrow(
					() -> new ResourceNotFoundException("해당 사용자를 찾을 수 없습니다. ID: " + userId));
		} catch (NumberFormatException e) {
			throw new InvalidInputValueException("유효하지 않은 사용자 ID 형식입니다.");
		}
	}

	/**
	 * 주문 ID 생성
	 * 형식: {공간코드}-{yyMMddHHmmss}
	 */
	private String generateOrderId(String spaceName) {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");
		String spaceCode = generateSpaceCode(spaceName);
		return spaceCode + "-" + now.format(formatter);
	}

	/**
	 * 공간 코드 생성 (SHA-256 해시의 앞 3자리)
	 */
	private String generateSpaceCode(String spaceName) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(spaceName.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString().substring(0, 3).toUpperCase();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Could not generate hash", e);
		}
	}

	/**
	 * 예약 조회 및 접근 권한 검증 (관리자 또는 소유주)
	 */
	private Reservation findReservationAndVerifyAccess(User user, Long reservationId) {
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResourceNotFoundException("해당 예약을 찾을 수 없습니다."));

		List<Integer> adminRoles = Arrays.asList(0, 1, 2);
		boolean isOwner = reservation.getUser().getUserId().equals(user.getUserId());
		boolean isAdmin = adminRoles.contains(user.getUserRole().getRoleId());

		if (!isAdmin && !isOwner) {
			throw new ForbiddenAccessException("해당 예약에 대한 접근 권한이 없습니다.");
		}

		return reservation;
	}

	/**
	 * 예약 조회 및 소유권 검증 (소유주만)
	 */
	private Reservation findReservationAndVerifyOwnership(User user, Long reservationId) {
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResourceNotFoundException("해당 예약을 찾을 수 없습니다."));

		if (!reservation.getUser().getUserId().equals(user.getUserId())) {
			throw new ForbiddenAccessException("해당 예약에 대한 접근 권한이 없습니다.");
		}
		return reservation;
	}
}
