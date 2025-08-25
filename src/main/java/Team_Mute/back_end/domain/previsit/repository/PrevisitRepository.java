package Team_Mute.back_end.domain.previsit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;

@Repository
public interface PrevisitRepository extends JpaRepository<PrevisitReservation, Long> {

	boolean existsByReservationReservationId(Long reservationId);

	Optional<PrevisitReservation> findByReservationReservationId(Long reservationId);
}
