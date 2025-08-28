package Team_Mute.back_end.domain.reservation_admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReservationLog is a Querydsl query type for ReservationLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationLog extends EntityPathBase<ReservationLog> {

    private static final long serialVersionUID = -1211907969L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReservationLog reservationLog = new QReservationLog("reservationLog");

    public final Team_Mute.back_end.domain.reservation.entity.QReservationStatus changedStatus;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memo = createString("memo");

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final Team_Mute.back_end.domain.reservation.entity.QReservation reservation;

    public QReservationLog(String variable) {
        this(ReservationLog.class, forVariable(variable), INITS);
    }

    public QReservationLog(Path<? extends ReservationLog> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReservationLog(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReservationLog(PathMetadata metadata, PathInits inits) {
        this(ReservationLog.class, metadata, inits);
    }

    public QReservationLog(Class<? extends ReservationLog> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.changedStatus = inits.isInitialized("changedStatus") ? new Team_Mute.back_end.domain.reservation.entity.QReservationStatus(forProperty("changedStatus")) : null;
        this.reservation = inits.isInitialized("reservation") ? new Team_Mute.back_end.domain.reservation.entity.QReservation(forProperty("reservation"), inits.get("reservation")) : null;
    }

}

