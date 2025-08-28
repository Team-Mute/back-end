package Team_Mute.back_end.domain.space_admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSpaceOperation is a Querydsl query type for SpaceOperation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpaceOperation extends EntityPathBase<SpaceOperation> {

    private static final long serialVersionUID = 238690L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSpaceOperation spaceOperation = new QSpaceOperation("spaceOperation");

    public final NumberPath<Integer> day = createNumber("day", Integer.class);

    public final BooleanPath isOpen = createBoolean("isOpen");

    public final TimePath<java.time.LocalTime> operationFrom = createTime("operationFrom", java.time.LocalTime.class);

    public final NumberPath<Integer> operationId = createNumber("operationId", Integer.class);

    public final TimePath<java.time.LocalTime> operationTo = createTime("operationTo", java.time.LocalTime.class);

    public final QSpace space;

    public QSpaceOperation(String variable) {
        this(SpaceOperation.class, forVariable(variable), INITS);
    }

    public QSpaceOperation(Path<? extends SpaceOperation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSpaceOperation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSpaceOperation(PathMetadata metadata, PathInits inits) {
        this(SpaceOperation.class, metadata, inits);
    }

    public QSpaceOperation(Class<? extends SpaceOperation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.space = inits.isInitialized("space") ? new QSpace(forProperty("space")) : null;
    }

}

