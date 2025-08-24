package Team_Mute.back_end.domain.reservation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.reservation.entity.Reservation;

@Repository
// JpaRepository의 두 번째 제네릭 인자를 String에서 Long으로 변경합니다.
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	// 사용자로 예약 목록 조회 (페이지네이션 적용)
	Page<Reservation> findByUser(User user, Pageable pageable);

	// 이제 JpaRepository가 제공하는 findById(Long id)를 사용하므로
	// 추가적인 findBy~ 메서드는 필요 없습니다.
}
