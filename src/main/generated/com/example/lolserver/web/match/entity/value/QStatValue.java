package com.example.lolserver.web.match.entity.value;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStatValue is a Querydsl query type for StatValue
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QStatValue extends BeanPath<StatValue> {

    private static final long serialVersionUID = 1682980127L;

    public static final QStatValue statValue = new QStatValue("statValue");

    public final NumberPath<Integer> defense = createNumber("defense", Integer.class);

    public final NumberPath<Integer> flex = createNumber("flex", Integer.class);

    public final NumberPath<Integer> offense = createNumber("offense", Integer.class);

    public QStatValue(String variable) {
        super(StatValue.class, forVariable(variable));
    }

    public QStatValue(Path<? extends StatValue> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStatValue(PathMetadata metadata) {
        super(StatValue.class, metadata);
    }

}

