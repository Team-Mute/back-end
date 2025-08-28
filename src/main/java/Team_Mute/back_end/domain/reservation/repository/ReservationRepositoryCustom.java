package Team_Mute.back_end.domain.reservation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.reservation.entity.Reservation;

public interface ReservationRepositoryCustom {
	Page<Reservation> findReservationsByFilter(User user, String filterOption, Pageable pageable);
}
