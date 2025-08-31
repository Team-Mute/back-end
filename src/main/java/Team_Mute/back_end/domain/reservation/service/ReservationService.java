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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
import Team_Mute.back_end.domain.previsit.repository.PrevisitRepository;
import Team_Mute.back_end.domain.reservation.dto.request.ReservationRequestDto;
import Team_Mute.back_end.domain.reservation.dto.response.PagedReservationResponse;
import Team_Mute.back_end.domain.reservation.dto.response.RejectReasonResponseDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationCancelResponseDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationDetailResponseDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationListDto;
import Team_Mute.back_end.domain.reservation.dto.response.ReservationResponseDto;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation.exception.ForbiddenAccessException;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.exception.ResourceNotFoundException;
import Team_Mute.back_end.domain.reservation.repository.ReservationRepository;
import Team_Mute.back_end.domain.reservation.repository.ReservationStatusRepository;
import Team_Mute.back_end.domain.reservation_admin.entity.ReservationLog;
import Team_Mute.back_end.domain.reservation_admin.repository.ReservationLogRepository;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import Team_Mute.back_end.domain.space_admin.util.S3Deleter;
import Team_Mute.back_end.domain.space_admin.util.S3Uploader;
import lombok.RequiredArgsConstructor;

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

	@Transactional
	public ReservationResponseDto createReservation(String userId, ReservationRequestDto requestDto) {
		User user = findUserById(userId);

		if (user.getUserRole().getRoleId() != 3) {
			throw new ForbiddenAccessException("예약을 생성할 권한이 없습니다.");
		}

		final List<Long> validStatusIds = Arrays.asList(1L, 2L, 3L);

		Space space = spaceRepository.findById(requestDto.getSpaceId())
			.orElseThrow(() -> new ResourceNotFoundException("해당 공간을 찾을 수 없습니다."));

		// '본 예약' 시간이 겹치는지 확인 (상태 필터링 적용)
		boolean isReservationOverlapping = reservationRepository.existsOverlappingReservationWithStatus(
			space.getSpaceId(),
			requestDto.getReservationFrom(),
			requestDto.getReservationTo(),
			validStatusIds // 상태 ID 리스트 전달
		);

		// '사전 답사 예약' 시간이 겹치는지 확인 (상태 필터링 적용)
		boolean isPrevisitOverlapping = previsitReservationRepository.existsOverlappingPrevisitWithStatus(
			space.getSpaceId(),
			requestDto.getReservationFrom(),
			requestDto.getReservationTo(),
			validStatusIds // 상태 ID 리스트 전달
		);

		// 4. 두 예약 중 하나라도 겹치면 예외 발생
		if (isReservationOverlapping || isPrevisitOverlapping) {
			throw new DataIntegrityViolationException("해당 시간에는 확정된 예약 또는 사전 답사가 존재하여 예약할 수 없습니다.");
		}

		final Long INITIAL_RESERVATION_STATUS_ID = 1L;
		ReservationStatus status = reservationStatusRepository.findById(INITIAL_RESERVATION_STATUS_ID)
			.orElseThrow(
				() -> new ResourceNotFoundException("기본 예약 상태(ID: " + INITIAL_RESERVATION_STATUS_ID + ")를 찾을 수 없습니다."));

		// 1. 첨부파일 정보 없이 예약 객체 먼저 생성 및 저장 (ID 확보 목적)
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

		Reservation savedReservation = reservationRepository.save(reservation); // ID를 즉시 할당받기 위해 flush

		// 2. 파일 업로드 및 URL 저장
		List<String> attachmentUrls = new ArrayList<>();
		if (requestDto.getReservationAttachments() != null && !requestDto.getReservationAttachments().isEmpty()) {
			// S3 디렉토리 경로: attachment/{reservation_id}/
			String dirName = "attachment/" + savedReservation.getReservationId();
			attachmentUrls = s3Uploader.uploadAll(requestDto.getReservationAttachments(), dirName);
		}

		// 3. 파일 URL 리스트를 예약 정보에 업데이트
		savedReservation.setReservationAttachment(attachmentUrls);

		// 변경된 내용을 최종 저장
		Reservation finalReservation = reservationRepository.save(savedReservation);

		return ReservationResponseDto.fromEntity(finalReservation);
	}

	@Transactional(readOnly = true)
	public PagedReservationResponse findReservations(String userId, String filterOption, Pageable pageable) {
		User user = findUserById(userId);

		// 2. 일반 사용자만 접근 가능하도록 권한 확인
		if (user.getUserRole().getRoleId() != 3) {
			throw new ForbiddenAccessException("일반 사용자만 접근 가능한 기능입니다.");
		}

		// pageable이 null이면 unpaged, 아니면 받은 값 사용
		Pageable pageableToUse = (pageable != null) ? pageable : Pageable.unpaged();

		// 3, 4. 필터 옵션에 따라 동적 쿼리 실행
		Page<Reservation> reservationPage = reservationRepository.findReservationsByFilter(user, filterOption,
			pageableToUse);

		// 5. 응답 DTO로 변환
		Page<ReservationListDto> dtoPage = reservationPage.map(ReservationListDto::fromEntity);

		return PagedReservationResponse.fromPage(dtoPage);
	}

	@Transactional(readOnly = true)
	public ReservationDetailResponseDto findReservationById(String userId, Long reservationId) {
		User user = findUserById(userId);

		// 2. 일반 사용자(role_id=3)만 접근 가능하도록 권한 확인
		if (user.getUserRole().getRoleId() != 3) {
			throw new ForbiddenAccessException("일반 사용자만 접근 가능한 기능입니다.");
		}

		// findReservationAndVerifyAccess는 관리자 또는 소유주만 허용하므로,
		// 일반 사용자이면서 소유주인 경우만 통과시키기 위해 findReservationAndVerifyOwnership 사용
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		// 새로운 DTO로 변환하여 반환
		return ReservationDetailResponseDto.fromEntity(reservation);
	}

	@Transactional
	public ReservationResponseDto updateReservation(String userId, Long reservationId,
		ReservationRequestDto requestDto) {
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		// 1. 예약 상태 검증 (기존 로직 유지)
		Long currentStatusId = reservation.getReservationStatus().getReservationStatusId();
		if (!Arrays.asList(4L, 6L).contains(currentStatusId)) {
			throw new IllegalArgumentException("반려 또는 취소된 예약만 수정할 수 있습니다.");
		}

		final List<Long> validStatusIds = Arrays.asList(1L, 2L, 3L);

		// 2. 비관적 락을 걸어 Space 엔티티를 조회 (예약 생성 시와 동일)
		// SpaceRepository의 findById에 @Lock(LockModeType.PESSIMISTIC_WRITE)가 적용되어 있어야 합니다.
		Space space = spaceRepository.findById(requestDto.getSpaceId())
			.orElseThrow(() -> new ResourceNotFoundException("해당 공간을 찾을 수 없습니다."));

		// 3. 자기 자신을 제외한 '본 예약' 시간이 겹치는지 확인
		boolean isReservationOverlapping = reservationRepository.existsOverlappingReservationExcludingSelf(
			space.getSpaceId(),
			requestDto.getReservationFrom(),
			requestDto.getReservationTo(),
			validStatusIds,
			reservationId // 자기 자신 ID를 제외하도록 전달
		);

		// 4. 자기 자신을 제외한 '사전 답사 예약' 시간이 겹치는지 확인
		boolean isPrevisitOverlapping = previsitReservationRepository.existsOverlappingPrevisitExcludingSelf(
			space.getSpaceId(),
			requestDto.getReservationFrom(),
			requestDto.getReservationTo(),
			validStatusIds,
			reservationId // 자기 자신 ID를 제외하도록 전달
		);

		// 5. 두 예약 중 하나라도 겹치면 예외 발생
		if (isReservationOverlapping || isPrevisitOverlapping) {
			throw new DataIntegrityViolationException("변경하려는 시간에는 이미 다른 확정된 예약 또는 사전 답사가 존재합니다.");
		}
		// 6. 파일 처리
		List<String> currentAttachments = new ArrayList<>(reservation.getReservationAttachment());
		List<String> existingAttachments = requestDto.getExistingAttachments() != null ?
			requestDto.getExistingAttachments() : new ArrayList<>();
		currentAttachments.removeAll(existingAttachments);
		if (!currentAttachments.isEmpty()) {
			currentAttachments.forEach(s3Deleter::deleteByUrl);
		}

		List<String> newAttachmentUrls = new ArrayList<>();
		List<MultipartFile> newFiles = requestDto.getReservationAttachments();
		if (newFiles != null && !newFiles.isEmpty()) {
			String dirName = "attachment/" + reservation.getReservationId();
			newAttachmentUrls = s3Uploader.uploadAll(newFiles, dirName);
		}

		List<String> finalAttachmentUrls = new ArrayList<>(existingAttachments);
		finalAttachmentUrls.addAll(newAttachmentUrls);

		// 7. 예약 정보 및 상태 업데이트
		final Long INITIAL_RESERVATION_STATUS_ID = 1L;
		ReservationStatus initialStatus = reservationStatusRepository.findById(INITIAL_RESERVATION_STATUS_ID)
			.orElseThrow(() -> new ResourceNotFoundException("기본 예약 상태를 찾을 수 없습니다."));

		reservation.updateDetails(
			space,
			initialStatus,
			requestDto.getReservationHeadcount(),
			requestDto.getReservationFrom(),
			requestDto.getReservationTo(),
			requestDto.getReservationPurpose(),
			finalAttachmentUrls
		);

		List<PrevisitReservation> previsits = reservation.getPrevisitReservations();
		if (previsits != null && !previsits.isEmpty()) {
			for (PrevisitReservation previsit : previsits) {
				previsit.setReservationStatusId(INITIAL_RESERVATION_STATUS_ID);
			}
		}

		Reservation updatedReservation = reservationRepository.save(reservation);

		return ReservationResponseDto.fromEntity(updatedReservation);
	}

	public void deleteReservation(String userId, Long reservationId) {
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		List<String> attachmentUrls = reservation.getReservationAttachment();
		if (attachmentUrls != null && !attachmentUrls.isEmpty()) {
			attachmentUrls.forEach(s3Deleter::deleteByUrl);
		}

		reservationRepository.delete(reservation);
	}

	public ReservationCancelResponseDto cancelReservation(String userId, Long reservationId) {
		// 1. 사용자 조회 및 예약 소유권 확인
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		// 2. 현재 예약 상태 및 취소 가능 여부 확인
		ReservationStatus currentStatus = reservation.getReservationStatus();
		Long currentStatusId = currentStatus.getReservationStatusId();
		List<Long> cancellableStatusIds = Arrays.asList(1L, 2L, 3L, 4L); // 1차 승인 대기, 2차 승인 대기, 최종 승인 완료, 반려됨

		if (!cancellableStatusIds.contains(currentStatusId)) {
			throw new IllegalArgumentException("이미 취소되었거나 이용 완료된 예약은 취소할 수 없습니다.");
		}

		// 3. '취소됨' 상태 객체 조회
		final Long CANCELLED_STATUS_ID = 6L;
		ReservationStatus cancelledStatus = reservationStatusRepository.findById(CANCELLED_STATUS_ID)
			.orElseThrow(
				() -> new ResourceNotFoundException("예약 상태 '취소됨'(ID: " + CANCELLED_STATUS_ID + ")을 찾을 수 없습니다."));

		// 4. 예약 상태를 '취소됨'으로 변경
		reservation.setReservationStatusId(cancelledStatus);
		reservationRepository.save(reservation);

		List<PrevisitReservation> previsits = reservation.getPrevisitReservations();
		if (previsits != null && !previsits.isEmpty()) {
			for (PrevisitReservation previsit : previsits) {
				previsit.setReservationStatusId(CANCELLED_STATUS_ID);
			}
		}

		// 5. 성공 응답 DTO 생성 및 반환
		return ReservationCancelResponseDto.builder()
			.reservationId(reservation.getReservationId())
			.fromStatus(currentStatus.getReservationStatusName())
			.toStatus(cancelledStatus.getReservationStatusName())
			.approvedAt(ZonedDateTime.now())
			.message("예약 상태 변경 성공")
			.build();
	}

	// --- Private Helper Methods ---

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

	private String generateOrderId(String spaceName) {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");
		String spaceCode = generateSpaceCode(spaceName);
		return spaceCode + "-" + now.format(formatter);
	}

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
			// "SHA-256"은 표준 알고리즘이므로 이 예외가 발생할 가능성은 거의 없습니다.
			// 발생 시 JRE 구성 문제일 수 있으므로 RuntimeException으로 처리합니다.
			throw new RuntimeException("Could not generate hash", e);
		}
	}

	private Reservation findReservationAndVerifyAccess(User user, Long reservationId) {
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResourceNotFoundException("해당 예약을 찾을 수 없습니다."));

		List<Integer> adminRoles = Arrays.asList(0, 1, 2);
		boolean isOwner = reservation.getUser().getUserId().equals(user.getUserId());
		boolean isAdmin = adminRoles.contains(user.getUserRole().getRoleId());

		// 관리자이거나 예약 소유주가 아니면 접근 불가
		if (!isAdmin && !isOwner) {
			throw new ForbiddenAccessException("해당 예약에 대한 접근 권한이 없습니다.");
		}

		return reservation;
	}

	private Reservation findReservationAndVerifyOwnership(User user, Long reservationId) {
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResourceNotFoundException("해당 예약을 찾을 수 없습니다."));

		if (!reservation.getUser().getUserId().equals(user.getUserId())) {
			throw new ForbiddenAccessException("해당 예약에 대한 접근 권한이 없습니다.");
		}
		return reservation;
	}

	@Transactional(readOnly = true)
	public RejectReasonResponseDto findRejectReason(String userId, Long reservationId) {
		// 1. 사용자 인증 및 예약 소유권 확인
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		// 2. 예약 상태가 '반려'(ID: 4)인지 확인
		final Long REJECTED_STATUS_ID = 4L;
		if (!reservation.getReservationStatus().getReservationStatusId().equals(REJECTED_STATUS_ID)) {
			throw new IllegalArgumentException("반려 상태의 예약이 아닙니다.");
		}

		// 3. 해당 예약의 '반려' 상태 로그 조회
		ReservationLog rejectLog = reservationLogRepository
			.findTopByReservationReservationIdAndChangedStatusReservationStatusIdOrderByRegDateDesc(
				reservationId, REJECTED_STATUS_ID)
			.orElseThrow(() -> new ResourceNotFoundException("반려 사유를 찾을 수 없습니다."));

		// 4. 로그에서 memo(반려 사유)를 추출하여 응답
		return new RejectReasonResponseDto(rejectLog.getMemo());
	}

}
