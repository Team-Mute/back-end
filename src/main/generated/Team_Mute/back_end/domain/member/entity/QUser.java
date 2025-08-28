package Team_Mute.back_end.domain.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -387777900L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final BooleanPath agreeEmail = createBoolean("agreeEmail");

    public final BooleanPath agreeLocation = createBoolean("agreeLocation");

    public final BooleanPath agreeSms = createBoolean("agreeSms");

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final NumberPath<Integer> tokenVer = createNumber("tokenVer", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updDate = createDateTime("updDate", java.time.LocalDateTime.class);

    public final QUserCompany userCompany;

    public final StringPath userEmail = createString("userEmail");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final StringPath userName = createString("userName");

    public final StringPath userPhone = createString("userPhone");

    public final StringPath userPwd = createString("userPwd");

    public final QUserRole userRole;

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.userCompany = inits.isInitialized("userCompany") ? new QUserCompany(forProperty("userCompany")) : null;
        this.userRole = inits.isInitialized("userRole") ? new QUserRole(forProperty("userRole")) : null;
    }

}

