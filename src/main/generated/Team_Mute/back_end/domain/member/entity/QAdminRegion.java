package Team_Mute.back_end.domain.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAdminRegion is a Querydsl query type for AdminRegion
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAdminRegion extends EntityPathBase<AdminRegion> {

    private static final long serialVersionUID = -24802246L;

    public static final QAdminRegion adminRegion = new QAdminRegion("adminRegion");

    public final ListPath<Admin, QAdmin> admin = this.<Admin, QAdmin>createList("admin", Admin.class, QAdmin.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final NumberPath<Integer> regionId = createNumber("regionId", Integer.class);

    public final StringPath regionName = createString("regionName");

    public final DateTimePath<java.time.LocalDateTime> updDate = createDateTime("updDate", java.time.LocalDateTime.class);

    public QAdminRegion(String variable) {
        super(AdminRegion.class, forVariable(variable));
    }

    public QAdminRegion(Path<? extends AdminRegion> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAdminRegion(PathMetadata metadata) {
        super(AdminRegion.class, metadata);
    }

}

