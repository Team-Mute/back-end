package Team_Mute.back_end.domain.space_admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSpace is a Querydsl query type for Space
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpace extends EntityPathBase<Space> {

    private static final long serialVersionUID = -966666619L;

    public static final QSpace space = new QSpace("space");

    public final NumberPath<Integer> categoryId = createNumber("categoryId", Integer.class);

    public final ListPath<SpaceClosedDay, QSpaceClosedDay> closedDays = this.<SpaceClosedDay, QSpaceClosedDay>createList("closedDays", SpaceClosedDay.class, QSpaceClosedDay.class, PathInits.DIRECT2);

    public final ListPath<SpaceImage, QSpaceImage> images = this.<SpaceImage, QSpaceImage>createList("images", SpaceImage.class, QSpaceImage.class, PathInits.DIRECT2);

    public final NumberPath<Integer> locationId = createNumber("locationId", Integer.class);

    public final ListPath<SpaceOperation, QSpaceOperation> operations = this.<SpaceOperation, QSpaceOperation>createList("operations", SpaceOperation.class, QSpaceOperation.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final NumberPath<Integer> regionId = createNumber("regionId", Integer.class);

    public final StringPath reservationWay = createString("reservationWay");

    public final NumberPath<Integer> spaceCapacity = createNumber("spaceCapacity", Integer.class);

    public final StringPath spaceDescription = createString("spaceDescription");

    public final NumberPath<Integer> spaceId = createNumber("spaceId", Integer.class);

    public final StringPath spaceImageUrl = createString("spaceImageUrl");

    public final BooleanPath spaceIsAvailable = createBoolean("spaceIsAvailable");

    public final StringPath spaceName = createString("spaceName");

    public final StringPath spaceRules = createString("spaceRules");

    public final ListPath<SpaceTagMap, QSpaceTagMap> tagMaps = this.<SpaceTagMap, QSpaceTagMap>createList("tagMaps", SpaceTagMap.class, QSpaceTagMap.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> updDate = createDateTime("updDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QSpace(String variable) {
        super(Space.class, forVariable(variable));
    }

    public QSpace(Path<? extends Space> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSpace(PathMetadata metadata) {
        super(Space.class, metadata);
    }

}

