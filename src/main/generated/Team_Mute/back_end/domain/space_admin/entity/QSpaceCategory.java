package Team_Mute.back_end.domain.space_admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSpaceCategory is a Querydsl query type for SpaceCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpaceCategory extends EntityPathBase<SpaceCategory> {

    private static final long serialVersionUID = 412525219L;

    public static final QSpaceCategory spaceCategory = new QSpaceCategory("spaceCategory");

    public final NumberPath<Integer> categoryId = createNumber("categoryId", Integer.class);

    public final StringPath categoryName = createString("categoryName");

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> updDate = createDateTime("updDate", java.time.LocalDateTime.class);

    public QSpaceCategory(String variable) {
        super(SpaceCategory.class, forVariable(variable));
    }

    public QSpaceCategory(Path<? extends SpaceCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSpaceCategory(PathMetadata metadata) {
        super(SpaceCategory.class, metadata);
    }

}

