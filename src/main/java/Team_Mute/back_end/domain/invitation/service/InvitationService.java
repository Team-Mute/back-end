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

/**
 * 초대장 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 완료된 예약에 대한 초대장 상세 정보 조회 기능을 제공
 * 예약 정보, 공간 정보, 위치 정보를 결합하여 초대장 데이터를 생성
 *
 * @author Team Mute
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class InvitationService {

	private final ReservationRepository reservationRepository;
	private final SpaceLocationRepository spaceLocationRepository;

	/**
	 * 초대장 상세 정보 조회
	 * - 예약 ID를 기반으로 예약 정보, 공간 정보, 위치 정보를 조회하여 초대장 DTO로 변환
	 * - 예약자명, 공간명, 주소, 예약 일정, 목적, 첨부 파일 등 초대장에 필요한 모든 정보를 포함
	 *
	 * @param reservationId 조회할 예약의 고유 식별자
	 * @return 초대장 상세 정보를 담고 있는 {@code InvitationResponseDto}
	 * @throws ResourceNotFoundException 예약 정보 또는 공간 위치 정보를 찾을 수 없는 경우 발생
	 */
	@Transactional(readOnly = true)
	public InvitationResponseDto getInvitationDetails(Long reservationId) {

		// 1. reservationId로 예약 정보 조회
		Reservation reservation = reservationRepository.findByReservationId(reservationId)
			.orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));

		// 2. 예약과 연관된 공간 정보 추출
		Space space = reservation.getSpace();
		Integer locationId = space.getLocationId();

		// 3. locationId로 SpaceLocation 정보 조회 (도로명 주소 획득)
		SpaceLocation location = spaceLocationRepository.findById(locationId)
			.orElseThrow(() -> new ResourceNotFoundException("Location not found for id: " + locationId));

		String addressRoad = location.getAddressRoad();

		// 4. DTO 빌더를 사용하여 초대장 응답 객체 생성 및 반환
		return InvitationResponseDto.builder()
			.userName(reservation.getUser().getUserName())
			.spaceName(space.getSpaceName())
			.addressRoad(addressRoad)
			.reservationFrom(reservation.getReservationFrom())
			.reservationTo(reservation.getReservationTo())
			.reservationPurpose(reservation.getReservationPurpose())
			.reservationAttachment(reservation.getReservationAttachment())
			.build();
	}
}
