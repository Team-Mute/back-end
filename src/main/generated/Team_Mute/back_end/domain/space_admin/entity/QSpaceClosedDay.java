package Team_Mute.back_end.domain.space_admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSpaceClosedDay is a Querydsl query type for SpaceClosedDay
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpaceClosedDay extends EntityPathBase<SpaceClosedDay> {

    private static final long serialVersionUID = -2144505461L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSpaceClosedDay spaceClosedDay = new QSpaceClosedDay("spaceClosedDay");

    public final DateTimePath<java.time.LocalDateTime> closedFrom = createDateTime("closedFrom", java.time.LocalDateTime.class);

    public final NumberPath<Integer> closedId = createNumber("closedId", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> closedTo = createDateTime("closedTo", java.time.LocalDateTime.class);

    public final QSpace space;

    public QSpaceClosedDay(String variable) {
        this(SpaceClosedDay.class, forVariable(variable), INITS);
    }

    public QSpaceClosedDay(Path<? extends SpaceClosedDay> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSpaceClosedDay(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSpaceClosedDay(PathMetadata metadata, PathInits inits) {
        this(SpaceClosedDay.class, metadata, inits);
    }

    public QSpaceClosedDay(Class<? extends SpaceClosedDay> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.space = inits.isInitialized("space") ? new QSpace(forProperty("space")) : null;
    }

}

