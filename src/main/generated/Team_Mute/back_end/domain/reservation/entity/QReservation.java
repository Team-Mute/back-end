package Team_Mute.back_end.domain.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReservation is a Querydsl query type for Reservation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservation extends EntityPathBase<Reservation> {

    private static final long serialVersionUID = -1189640875L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReservation reservation = new QReservation("reservation");

    public final StringPath orderId = createString("orderId");

    public final QPrevisitReservation previsitReservation;

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final ListPath<String, StringPath> reservationAttachment = this.<String, StringPath>createList("reservationAttachment", String.class, StringPath.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> reservationFrom = createDateTime("reservationFrom", java.time.LocalDateTime.class);

    public final NumberPath<Integer> reservationHeadcount = createNumber("reservationHeadcount", Integer.class);

    public final NumberPath<Long> reservationId = createNumber("reservationId", Long.class);

    public final StringPath reservationPurpose = createString("reservationPurpose");

    public final QReservationStatus reservationStatus;

    public final DateTimePath<java.time.LocalDateTime> reservationTo = createDateTime("reservationTo", java.time.LocalDateTime.class);

    public final Team_Mute.back_end.domain.space_admin.entity.QSpace space;

    public final DateTimePath<java.time.LocalDateTime> updDate = createDateTime("updDate", java.time.LocalDateTime.class);

    public final Team_Mute.back_end.domain.member.entity.QUser user;

    public QReservation(String variable) {
        this(Reservation.class, forVariable(variable), INITS);
    }

    public QReservation(Path<? extends Reservation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReservation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReservation(PathMetadata metadata, PathInits inits) {
        this(Reservation.class, metadata, inits);
    }

    public QReservation(Class<? extends Reservation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.previsitReservation = inits.isInitialized("previsitReservation") ? new QPrevisitReservation(forProperty("previsitReservation"), inits.get("previsitReservation")) : null;
        this.reservationStatus = inits.isInitialized("reservationStatus") ? new QReservationStatus(forProperty("reservationStatus")) : null;
        this.space = inits.isInitialized("space") ? new Team_Mute.back_end.domain.space_admin.entity.QSpace(forProperty("space")) : null;
        this.user = inits.isInitialized("user") ? new Team_Mute.back_end.domain.member.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

