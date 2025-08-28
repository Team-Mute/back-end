package Team_Mute.back_end.domain.space_admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSpaceTag is a Querydsl query type for SpaceTag
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpaceTag extends EntityPathBase<SpaceTag> {

    private static final long serialVersionUID = -209443115L;

    public static final QSpaceTag spaceTag = new QSpaceTag("spaceTag");

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final NumberPath<Integer> tagId = createNumber("tagId", Integer.class);

    public final StringPath tagName = createString("tagName");

    public final DateTimePath<java.time.LocalDateTime> updDate = createDateTime("updDate", java.time.LocalDateTime.class);

    public QSpaceTag(String variable) {
        super(SpaceTag.class, forVariable(variable));
    }

    public QSpaceTag(Path<? extends SpaceTag> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSpaceTag(PathMetadata metadata) {
        super(SpaceTag.class, metadata);
    }

}

