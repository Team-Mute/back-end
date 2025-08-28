package Team_Mute.back_end.domain.reservation.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.reservation.entity.QReservation;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepositoryCustom {

	private final JPAQueryFactory queryFactory;
	private final QReservation reservation = QReservation.reservation;

	@Override
	public Page<Reservation> findReservationsByFilter(User user, String filterOption, Pageable pageable) {
		List<Long> statusIds = getStatusIdsByFilter(filterOption);

		BooleanExpression predicate = reservation.user.eq(user);
		if (statusIds != null && !statusIds.isEmpty()) {
			predicate = predicate.and(reservation.reservationStatus.reservationStatusId.in(statusIds));
		}

		List<Reservation> content = queryFactory
			.selectFrom(reservation)
			.where(predicate)
			.orderBy(getOrderBySpecifier(filterOption))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		Long total = queryFactory
			.select(reservation.count())
			.from(reservation)
			.where(predicate)
			.fetchOne();

		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	private List<Long> getStatusIdsByFilter(String filterOption) {
		if (filterOption == null)
			return null;
		return switch (filterOption) {
			case "진행중" -> List.of(1L, 2L);
			case "예약완료" -> List.of(3L);
			case "이용완료" -> List.of(5L);
			case "취소" -> List.of(4L, 6L);
			default -> null;
		};
	}

	private OrderSpecifier<?> getOrderBySpecifier(String filterOption) {
		if ("취소" .equals(filterOption)) {
			// 6(취소됨)이 4(반려)보다 앞으로 오도록 내림차순 정렬
			return new OrderSpecifier<>(Order.ASC, reservation.reservationStatus.reservationStatusId);
		}
		// 기본 정렬: 최신순
		return new OrderSpecifier<>(Order.DESC, reservation.regDate);
	}
}
