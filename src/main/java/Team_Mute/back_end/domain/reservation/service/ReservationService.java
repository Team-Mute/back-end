package Team_Mute.back_end.domain.reservation.service;

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
import org.springframework.web.multipart.MultipartFile;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
import Team_Mute.back_end.domain.reservation.dto.request.ReservationRequestDto;
import Team_Mute.back_end.domain.reservation.dto.response.PagedReservationResponse;
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

	public ReservationResponseDto createReservation(String userId, ReservationRequestDto requestDto) {
		User user = findUserById(userId);

		if (user.getUserRole().getRoleId() != 3) {
			throw new ForbiddenAccessException("예약을 생성할 권한이 없습니다.");
		}

		Space space = spaceRepository.findById(requestDto.getSpaceId())
			.orElseThrow(() -> new ResourceNotFoundException("해당 공간을 찾을 수 없습니다."));

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
			.reservationAttachment(new ArrayList<>()) // 빈 리스트로 초기화
			.build();

		Reservation savedReservation = reservationRepository.saveAndFlush(reservation); // ID를 즉시 할당받기 위해 flush

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

		// 3, 4. 필터 옵션에 따라 동적 쿼리 실행
		Page<Reservation> reservationPage = reservationRepository.findReservationsByFilter(user, filterOption,
			pageable);

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

	public ReservationResponseDto updateReservation(String userId, Long reservationId,
		ReservationRequestDto requestDto) {
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		// 1. 예약 상태 검증 (4: 반려됨, 6: 취소됨 상태일 때만 통과)
		Long currentStatusId = reservation.getReservationStatus().getReservationStatusId();
		List<Long> modifiableStatusIds = Arrays.asList(4L, 6L);

		if (!modifiableStatusIds.contains(currentStatusId)) {
			throw new IllegalArgumentException("반려 또는 취소된 예약만 수정할 수 있습니다.");
		}

		// ... (파일 처리 로직: 삭제, 업로드, 최종 목록 생성 - 이전과 동일) ...
		// 2. 삭제할 파일 결정 및 S3에서 삭제
		List<String> currentAttachments = new ArrayList<>(reservation.getReservationAttachment());
		List<String> existingAttachments = requestDto.getExistingAttachments() != null ?
			requestDto.getExistingAttachments() : new ArrayList<>();
		currentAttachments.removeAll(existingAttachments);
		if (!currentAttachments.isEmpty()) {
			currentAttachments.forEach(s3Deleter::deleteByUrl);
		}

		// 3. 새로운 파일 업로드
		List<String> newAttachmentUrls = new ArrayList<>();
		List<MultipartFile> newFiles = requestDto.getReservationAttachments();
		if (newFiles != null && !newFiles.isEmpty()) {
			String dirName = "attachment/" + reservation.getReservationId();
			newAttachmentUrls = s3Uploader.uploadAll(newFiles, dirName);
		}

		// 4. 최종 파일 목록 생성
		List<String> finalAttachmentUrls = new ArrayList<>(existingAttachments);
		finalAttachmentUrls.addAll(newAttachmentUrls);

		// 5. 예약 정보 및 상태 업데이트
		Space space = spaceRepository.findById(requestDto.getSpaceId())
			.orElseThrow(() -> new ResourceNotFoundException("해당 공간을 찾을 수 없습니다."));

		// '1차 승인 대기' 상태 객체 조회 (재사용 목적)
		final Long INITIAL_RESERVATION_STATUS_ID = 1L;
		ReservationStatus initialStatus = reservationStatusRepository.findById(INITIAL_RESERVATION_STATUS_ID)
			.orElseThrow(
				() -> new ResourceNotFoundException("기본 예약 상태(ID: " + INITIAL_RESERVATION_STATUS_ID + ")를 찾을 수 없습니다."));

		// 메인 예약 정보 업데이트
		reservation.updateDetails(
			space,
			initialStatus, // 상태를 '1차 승인 대기'로 설정
			requestDto.getReservationHeadcount(),
			requestDto.getReservationFrom(),
			requestDto.getReservationTo(),
			requestDto.getReservationPurpose(),
			finalAttachmentUrls
		);

		// 6. 연결된 사전 방문(previsit)의 상태도 '1차 승인 대기'로 변경
		List<PrevisitReservation> previsits = reservation.getPrevisitReservations();
		if (previsits != null && !previsits.isEmpty()) {
			for (PrevisitReservation previsit : previsits) {
				// PrevisitReservation 엔티티에 ReservationStatus를 설정하는 setter가 있다고 가정
				previsit.setReservationStatusId(INITIAL_RESERVATION_STATUS_ID);
			}
		}

		// 7. 변경된 예약 정보 최종 저장
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
		return spaceName + "-" + now.format(formatter);
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
}
