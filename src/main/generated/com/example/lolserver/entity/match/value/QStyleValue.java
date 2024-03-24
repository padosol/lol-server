package com.example.lolserver.entity.match.value;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStyleValue is a Querydsl query type for StyleValue
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QStyleValue extends BeanPath<StyleValue> {

    private static final long serialVersionUID = 1419399390L;

    public static final QStyleValue styleValue = new QStyleValue("styleValue");

    public final NumberPath<Integer> primaryRuneId = createNumber("primaryRuneId", Integer.class);

    public final StringPath primaryRuneIds = createString("primaryRuneIds");

    public final NumberPath<Integer> secondaryRuneId = createNumber("secondaryRuneId", Integer.class);

    public final StringPath secondaryRuneIds = createString("secondaryRuneIds");

    public QStyleValue(String variable) {
        super(StyleValue.class, forVariable(variable));
    }

    public QStyleValue(Path<? extends StyleValue> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStyleValue(PathMetadata metadata) {
        super(StyleValue.class, metadata);
    }

}

