package Team_Mute.back_end.domain.previsit.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPrevisitReservation is a Querydsl query type for PrevisitReservation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPrevisitReservation extends EntityPathBase<PrevisitReservation> {

    private static final long serialVersionUID = 56450573L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPrevisitReservation previsitReservation = new QPrevisitReservation("previsitReservation");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> previsitFrom = createDateTime("previsitFrom", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> previsitTo = createDateTime("previsitTo", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final Team_Mute.back_end.domain.reservation.entity.QReservation reservation;

    public final DateTimePath<java.time.LocalDateTime> updDate = createDateTime("updDate", java.time.LocalDateTime.class);

    public QPrevisitReservation(String variable) {
        this(PrevisitReservation.class, forVariable(variable), INITS);
    }

    public QPrevisitReservation(Path<? extends PrevisitReservation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPrevisitReservation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPrevisitReservation(PathMetadata metadata, PathInits inits) {
        this(PrevisitReservation.class, metadata, inits);
    }

    public QPrevisitReservation(Class<? extends PrevisitReservation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reservation = inits.isInitialized("reservation") ? new Team_Mute.back_end.domain.reservation.entity.QReservation(forProperty("reservation"), inits.get("reservation")) : null;
    }

}

