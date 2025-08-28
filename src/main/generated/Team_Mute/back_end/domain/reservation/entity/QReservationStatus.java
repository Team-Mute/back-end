package Team_Mute.back_end.domain.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReservationStatus is a Querydsl query type for ReservationStatus
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationStatus extends EntityPathBase<ReservationStatus> {

    private static final long serialVersionUID = 222571943L;

    public static final QReservationStatus reservationStatus = new QReservationStatus("reservationStatus");

    public final NumberPath<Long> reservationStatusId = createNumber("reservationStatusId", Long.class);

    public final StringPath reservationStatusName = createString("reservationStatusName");

    public QReservationStatus(String variable) {
        super(ReservationStatus.class, forVariable(variable));
    }

    public QReservationStatus(Path<? extends ReservationStatus> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReservationStatus(PathMetadata metadata) {
        super(ReservationStatus.class, metadata);
    }

}

