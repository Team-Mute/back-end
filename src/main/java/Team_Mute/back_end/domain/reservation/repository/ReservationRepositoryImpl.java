package Team_Mute.back_end.domain.reservation.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.reservation.entity.QReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.RequiredArgsConstructor;

/**
 * 예약 커스텀 리포지토리 구현체
 * QueryDSL을 사용하여 동적 쿼리 및 복잡한 조회 로직 처리
 */
@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepositoryCustom {

	private final JPAQueryFactory queryFactory;
	private final QReservation reservation = QReservation.reservation;

	/**
	 * 필터 옵션에 따라 예약 목록 조회 (페이징 지원)
	 *
	 * 필터 옵션:
	 * - "진행중": 상태 1, 2 (1차, 2차 승인 대기)
	 * - "예약완료": 상태 3 (최종 승인)
	 * - "이용완료": 상태 5
	 * - "취소": 상태 4, 6 (반려, 예약 취소)
	 * - null: 전체 조회
	 *
	 * @param user 사용자 엔티티
	 * @param filterOption 필터 옵션
	 * @param pageable 페이징 정보 (null이면 전체 조회)
	 * @return 예약 페이지
	 */
	@Override
	public Page<Reservation> findReservationsByFilter(User user, String filterOption, Pageable pageable) {
		// 1. 필터 옵션에 따라 상태 ID 리스트 가져오기
		List<Integer> statusIds = getStatusIdsByFilter(filterOption);

		// 2. 기본 조건: 사용자 일치
		BooleanExpression predicate = reservation.user.eq(user);

		// 3. 상태 필터링 조건 추가
		if (statusIds != null && !statusIds.isEmpty()) {
			predicate = predicate.and(reservation.reservationStatus.reservationStatusId.in((Number)statusIds));
		}

		// 4. 기본 쿼리 생성 (정렬 포함)
		JPAQuery<Reservation> query = queryFactory
			.selectFrom(reservation)
			.where(predicate)
			.orderBy(getOrderBySpecifier(filterOption));

		// 5. 페이징 여부에 따라 분기 처리
		if (pageable.isPaged()) {
			query.offset(pageable.getOffset())
				.limit(pageable.getPageSize());
		}

		// 6. 쿼리 실행
		List<Reservation> content = query.fetch();

		// 7. 전체 카운트 조회 (페이징과 무관하게 항상 필요)
		Long total = queryFactory
			.select(reservation.count())
			.from(reservation)
			.where(predicate)
			.fetchOne();

		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	/**
	 * 필터 옵션을 예약 상태 ID 리스트로 변환
	 * @param filterOption 필터 옵션 문자열
	 * @return 상태 ID 리스트 (null이면 필터링 없음)
	 */
	private List<Integer> getStatusIdsByFilter(String filterOption) {
		if (filterOption == null)
			return null;
		return switch (filterOption) {
			case "진행중" -> List.of(1, 2);      // 승인 대기, 1차 승인
			case "예약완료" -> List.of(3);       // 최종 승인
			case "이용완료" -> List.of(5);       // 이용 완료
			case "취소" -> List.of(4, 6);        // 반려, 예약 취소
			default -> null;
		};
	}

	/**
	 * 필터 옵션에 따라 정렬 방식 결정
	 * @param filterOption 필터 옵션
	 * @return QueryDSL OrderSpecifier
	 */
	private OrderSpecifier<?> getOrderBySpecifier(String filterOption) {
		if ("취소".equals(filterOption)) {
			// "취소" 필터: 상태 ID 오름차순 (6이 4보다 먼저)
			return new OrderSpecifier<>(Order.ASC, reservation.reservationStatus.reservationStatusId);
		}
		// 기본 정렬: 등록일 최신순
		return new OrderSpecifier<>(Order.DESC, reservation.regDate);
	}
}
