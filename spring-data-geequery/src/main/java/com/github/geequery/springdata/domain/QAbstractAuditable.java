package com.github.geequery.springdata.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QAbstractAuditable is a Querydsl query type for AbstractAuditable
 */
@Generated("com.querydsl.codegen.SupertypeSerializer")
public class QAbstractAuditable extends EntityPathBase<AbstractAuditable<?, ? extends java.io.Serializable>> {

    private static final long serialVersionUID = 1561562297L;

    public static final QAbstractAuditable abstractAuditable = new QAbstractAuditable("abstractAuditable");

    public final QAbstractPersistable _super = new QAbstractPersistable(this);

    public final SimplePath<Object> createdBy = createSimple("createdBy", Object.class);

    public final DateTimePath<java.util.Date> createdDate = createDateTime("createdDate", java.util.Date.class);

    //inherited
    public final SimplePath<java.io.Serializable> id = _super.id;

    public final SimplePath<Object> lastModifiedBy = createSimple("lastModifiedBy", Object.class);

    public final DateTimePath<java.util.Date> lastModifiedDate = createDateTime("lastModifiedDate", java.util.Date.class);

    @SuppressWarnings({"all", "rawtypes", "unchecked"})
    public QAbstractAuditable(String variable) {
        super((Class) AbstractAuditable.class, forVariable(variable));
    }

    @SuppressWarnings({"all", "rawtypes", "unchecked"})
    public QAbstractAuditable(Path<? extends AbstractAuditable> path) {
        super((Class) path.getType(), path.getMetadata());
    }

    @SuppressWarnings({"all", "rawtypes", "unchecked"})
    public QAbstractAuditable(PathMetadata metadata) {
        super((Class) AbstractAuditable.class, metadata);
    }

}

