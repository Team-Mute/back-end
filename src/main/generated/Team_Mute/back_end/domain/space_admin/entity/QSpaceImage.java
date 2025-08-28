package Team_Mute.back_end.domain.space_admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSpaceImage is a Querydsl query type for SpaceImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpaceImage extends EntityPathBase<SpaceImage> {

    private static final long serialVersionUID = 578825686L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSpaceImage spaceImage = new QSpaceImage("spaceImage");

    public final NumberPath<Integer> imageId = createNumber("imageId", Integer.class);

    public final NumberPath<Integer> imagePriority = createNumber("imagePriority", Integer.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final DateTimePath<java.sql.Timestamp> regDate = createDateTime("regDate", java.sql.Timestamp.class);

    public final QSpace space;

    public final DateTimePath<java.sql.Timestamp> updDate = createDateTime("updDate", java.sql.Timestamp.class);

    public QSpaceImage(String variable) {
        this(SpaceImage.class, forVariable(variable), INITS);
    }

    public QSpaceImage(Path<? extends SpaceImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSpaceImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSpaceImage(PathMetadata metadata, PathInits inits) {
        this(SpaceImage.class, metadata, inits);
    }

    public QSpaceImage(Class<? extends SpaceImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.space = inits.isInitialized("space") ? new QSpace(forProperty("space")) : null;
    }

}

