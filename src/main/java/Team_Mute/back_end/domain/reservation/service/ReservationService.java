package Team_Mute.back_end.domain.reservation.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.reservation.dto.request.ReservationRequestDto;
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
	public Page<ReservationResponseDto> findReservations(String userId, int currentPage, int limit) {
		User user = findUserById(userId);
		Pageable pageable = PageRequest.of(currentPage - 1, limit);
		Page<Reservation> reservationPage;

		List<Integer> adminRoles = Arrays.asList(0, 1, 2);
		if (adminRoles.contains(user.getUserRole().getRoleId())) {
			reservationPage = reservationRepository.findAll(pageable);
		} else if (user.getUserRole().getRoleId() == 3) {
			reservationPage = reservationRepository.findByUser(user, pageable);
		} else {
			throw new ForbiddenAccessException("예약을 조회할 권한이 없습니다.");
		}

		return reservationPage.map(ReservationResponseDto::fromEntity);
	}

	@Transactional(readOnly = true)
	public ReservationResponseDto findReservationById(String userId, Long reservationId) {
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyAccess(user, reservationId);
		return ReservationResponseDto.fromEntity(reservation);
	}

	public ReservationResponseDto updateReservation(String userId, Long reservationId,
		ReservationRequestDto requestDto) {
		User user = findUserById(userId);
		Reservation reservation = findReservationAndVerifyOwnership(user, reservationId);

		// 1. 기존 S3에 저장된 첨부파일 삭제
		List<String> oldAttachmentUrls = reservation.getReservationAttachment();
		if (oldAttachmentUrls != null && !oldAttachmentUrls.isEmpty()) {
			oldAttachmentUrls.forEach(s3Deleter::deleteByUrl);
		}

		// 2. 새로운 파일 업로드
		List<String> newAttachmentUrls = new ArrayList<>();
		List<MultipartFile> newFiles = requestDto.getReservationAttachments();

		if (newFiles != null && !newFiles.isEmpty()) {
			String dirName = "attachment/" + reservation.getReservationId();
			newAttachmentUrls = s3Uploader.uploadAll(newFiles, dirName);
		}

		// 3. 예약 정보 업데이트 (파일 URL 포함)
		Space space = spaceRepository.findById(requestDto.getSpaceId())
			.orElseThrow(() -> new ResourceNotFoundException("해당 공간을 찾을 수 없습니다."));

		reservation.updateDetails(
			space,
			reservation.getReservationStatus(), // 상태는 기존 값 유지
			requestDto.getReservationHeadcount(),
			requestDto.getReservationFrom(),
			requestDto.getReservationTo(),
			requestDto.getReservationPurpose(),
			newAttachmentUrls // 새로운 파일 URL 리스트로 교체
		);

		// 변경된 예약 정보 저장 (updateDetails가 영속성 컨텍스트 내에서 동작하므로 명시적 save는 선택사항)
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
