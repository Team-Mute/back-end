package Team_Mute.back_end.domain.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserCompany is a Querydsl query type for UserCompany
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserCompany extends EntityPathBase<UserCompany> {

    private static final long serialVersionUID = 1153272649L;

    public static final QUserCompany userCompany = new QUserCompany("userCompany");

    public final ListPath<Admin, QAdmin> admin = this.<Admin, QAdmin>createList("admin", Admin.class, QAdmin.class, PathInits.DIRECT2);

    public final NumberPath<Integer> companyId = createNumber("companyId", Integer.class);

    public final StringPath companyName = createString("companyName");

    public final DateTimePath<java.time.LocalDateTime> regDate = createDateTime("regDate", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> updDate = createDateTime("updDate", java.time.LocalDateTime.class);

    public final ListPath<User, QUser> users = this.<User, QUser>createList("users", User.class, QUser.class, PathInits.DIRECT2);

    public QUserCompany(String variable) {
        super(UserCompany.class, forVariable(variable));
    }

    public QUserCompany(Path<? extends UserCompany> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserCompany(PathMetadata metadata) {
        super(UserCompany.class, metadata);
    }

}

