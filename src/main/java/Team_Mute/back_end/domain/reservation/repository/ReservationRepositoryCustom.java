package Team_Mute.back_end.domain.reservation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.reservation.entity.Reservation;

/**
 * 예약 커스텀 리포지토리 인터페이스
 * QueryDSL을 사용한 동적 쿼리 메서드 정의
 * ReservationRepositoryImpl에서 구현
 */
public interface ReservationRepositoryCustom {
	/**
	 * 사용자의 예약 목록을 필터 옵션에 따라 조회 (페이징 지원)
	 * QueryDSL을 사용하여 동적으로 조건 생성
	 *
	 * @param user 사용자 엔티티
	 * @param filterOption 필터 옵션 ("진행중", "예약완료", "이용완료", "취소", null)
	 * @param pageable 페이징 정보 (null 가능 - 전체 조회)
	 * @return 예약 페이지
	 */
	Page<Reservation> findReservationsByFilter(User user, String filterOption, Pageable pageable);
}
