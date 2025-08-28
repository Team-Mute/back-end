package Team_Mute.back_end.domain.space_admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSpaceTagMap is a Querydsl query type for SpaceTagMap
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpaceTagMap extends EntityPathBase<SpaceTagMap> {

    private static final long serialVersionUID = 1067719239L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSpaceTagMap spaceTagMap = new QSpaceTagMap("spaceTagMap");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final QSpace space;

    public final QSpaceTag tag;

    public QSpaceTagMap(String variable) {
        this(SpaceTagMap.class, forVariable(variable), INITS);
    }

    public QSpaceTagMap(Path<? extends SpaceTagMap> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSpaceTagMap(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSpaceTagMap(PathMetadata metadata, PathInits inits) {
        this(SpaceTagMap.class, metadata, inits);
    }

    public QSpaceTagMap(Class<? extends SpaceTagMap> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.space = inits.isInitialized("space") ? new QSpace(forProperty("space")) : null;
        this.tag = inits.isInitialized("tag") ? new QSpaceTag(forProperty("tag")) : null;
    }

}

