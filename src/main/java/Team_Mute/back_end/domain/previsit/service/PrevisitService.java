package Team_Mute.back_end.domain.previsit.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Team_Mute.back_end.domain.previsit.dto.request.PrevisitCreateRequest;
import Team_Mute.back_end.domain.previsit.dto.request.PrevisitUpdateRequest;
import Team_Mute.back_end.domain.previsit.dto.response.PrevisitResponse;
import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
import Team_Mute.back_end.domain.previsit.exception.PrevisitAlreadyExistsException;
import Team_Mute.back_end.domain.previsit.repository.PrevisitRepository;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.exception.ForbiddenAccessException;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.exception.ResourceNotFoundException;
import Team_Mute.back_end.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PrevisitService {

	private final PrevisitRepository previsitRepository;
	private final ReservationRepository reservationRepository;

	public PrevisitResponse createPrevisit(PrevisitCreateRequest request) {
		validatePrevisitTimes(request.getPrevisitFrom(), request.getPrevisitTo());

		Long currentUserId = getCurrentUserId();
		Reservation reservation = findReservationById(request.getReservationId());

		if (!reservation.getUser().getUserId().equals(currentUserId)) {
			throw new ForbiddenAccessException("해당 예약에 대한 사전답사 생성 권한이 없습니다.");
		}

		if (previsitRepository.existsByReservationReservationId(request.getReservationId())) {
			throw new PrevisitAlreadyExistsException("해당 예약에 대한 사전답사 정보가 이미 존재합니다.");
		}

		PrevisitReservation previsit = new PrevisitReservation();
		previsit.setReservation(reservation);
		previsit.setReservationStatusId(reservation.getReservationStatus().getReservationStatusId());
		previsit.setPrevisitFrom(request.getPrevisitFrom());
		previsit.setPrevisitTo(request.getPrevisitTo());

		PrevisitReservation savedPrevisit = previsitRepository.save(previsit);
		return PrevisitResponse.from(savedPrevisit);
	}

	@Transactional(readOnly = true)
	public PrevisitResponse getPrevisitByReservationId(Long reservationId) {
		Long currentUserId = getCurrentUserId();
		PrevisitReservation previsit = previsitRepository.findByReservationReservationId(reservationId)
			.orElseThrow(() -> new ResourceNotFoundException("사전답사 정보를 찾을 수 없습니다."));

		if (!previsit.getReservation().getUser().getUserId().equals(currentUserId)) {
			throw new ForbiddenAccessException("해당 사전답사 정보를 조회할 권한이 없습니다.");
		}

		return PrevisitResponse.from(previsit);
	}

	public PrevisitResponse updatePrevisit(Long previsitId, PrevisitUpdateRequest request) {
		validatePrevisitTimes(request.getPrevisitFrom(), request.getPrevisitTo());

		Long currentUserId = getCurrentUserId();
		PrevisitReservation previsit = findPrevisitById(previsitId);

		if (!previsit.getReservation().getUser().getUserId().equals(currentUserId)) {
			throw new ForbiddenAccessException("해당 사전답사 정보를 수정할 권한이 없습니다.");
		}

		previsit.setPrevisitFrom(request.getPrevisitFrom());
		previsit.setPrevisitTo(request.getPrevisitTo());

		return PrevisitResponse.from(previsit);
	}

	public void deletePrevisit(Long previsitId) {
		Long currentUserId = getCurrentUserId();
		PrevisitReservation previsit = findPrevisitById(previsitId);

		if (!previsit.getReservation().getUser().getUserId().equals(currentUserId)) {
			throw new ForbiddenAccessException("해당 사전답사 정보를 삭제할 권한이 없습니다.");
		}

		previsitRepository.delete(previsit);
	}

	// --- Helper Methods ---

	private Long getCurrentUserId() {
		String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
		return Long.parseLong(userIdStr);
	}

	private Reservation findReservationById(Long reservationId) {
		return reservationRepository.findById(reservationId)
			.orElseThrow(() -> new ResourceNotFoundException("예약 정보를 찾을 수 없습니다. ID: " + reservationId));
	}

	private PrevisitReservation findPrevisitById(Long previsitId) {
		return previsitRepository.findById(previsitId)
			.orElseThrow(() -> new ResourceNotFoundException("사전답사 정보를 찾을 수 없습니다. ID: " + previsitId));
	}

	private void validatePrevisitTimes(java.time.LocalDateTime from, java.time.LocalDateTime to) {
		if (from.isAfter(to) || from.isEqual(to)) {
			throw new InvalidInputValueException("사전답사 시작 시간은 종료 시간보다 이전이어야 합니다.");
		}
	}
}
