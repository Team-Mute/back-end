package Team_Mute.back_end.domain.invitation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Team_Mute.back_end.domain.invitation.dto.response.InvitationResponseDto;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.exception.ResourceNotFoundException;
import Team_Mute.back_end.domain.reservation.repository.ReservationRepository;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.entity.SpaceLocation;
import Team_Mute.back_end.domain.space_admin.repository.SpaceLocationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationService {

	private final ReservationRepository reservationRepository;
	private final SpaceLocationRepository spaceLocationRepository; // Repository 주입

	@Transactional(readOnly = true)
	public InvitationResponseDto getInvitationDetails(Long reservationId) {
		// 1. reservationId로 예약 정보 조회
		Reservation reservation = reservationRepository.findByReservationId(reservationId)
			.orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));

		Space space = reservation.getSpace();
		Integer locationId = space.getLocationId(); // Space에서 locationId를 가져옵니다.

		// 2. locationId로 SpaceLocation 정보 조회
		SpaceLocation location = spaceLocationRepository.findById(locationId)
			.orElseThrow(() -> new ResourceNotFoundException("Location not found for id: " + locationId));

		String addressRoad = location.getAddressRoad();

		// 3. DTO 빌더를 사용하여 응답 객체 생성
		return InvitationResponseDto.builder()
			.userName(reservation.getUser().getUserName()) // User 엔티티에 getName()이 있다고 가정
			.spaceName(space.getSpaceName())
			.addressRoad(addressRoad)
			.reservationFrom(reservation.getReservationFrom())
			.reservationTo(reservation.getReservationTo())
			.reservationPurpose(reservation.getReservationPurpose())
			.reservationAttachment(reservation.getReservationAttachment())
			.build();
	}
}
