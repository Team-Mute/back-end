package Team_Mute.back_end.domain.space_admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSpaceLocation is a Querydsl query type for SpaceLocation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpaceLocation extends EntityPathBase<SpaceLocation> {

    private static final long serialVersionUID = -2031909542L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSpaceLocation spaceLocation = new QSpaceLocation("spaceLocation");

    public final StringPath addressRoad = createString("addressRoad");

    public final Team_Mute.back_end.domain.member.entity.QAdminRegion adminRegion;

    public final BooleanPath isActive = createBoolean("isActive");

    public final NumberPath<Integer> locationId = createNumber("locationId", Integer.class);

    public final StringPath locationName = createString("locationName");

    public final StringPath postalCode = createString("postalCode");

    public QSpaceLocation(String variable) {
        this(SpaceLocation.class, forVariable(variable), INITS);
    }

    public QSpaceLocation(Path<? extends SpaceLocation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSpaceLocation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSpaceLocation(PathMetadata metadata, PathInits inits) {
        this(SpaceLocation.class, metadata, inits);
    }

    public QSpaceLocation(Class<? extends SpaceLocation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.adminRegion = inits.isInitialized("adminRegion") ? new Team_Mute.back_end.domain.member.entity.QAdminRegion(forProperty("adminRegion")) : null;
    }

}

