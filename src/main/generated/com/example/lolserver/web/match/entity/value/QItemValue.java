package com.example.lolserver.web.match.entity.value;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QItemValue is a Querydsl query type for ItemValue
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QItemValue extends BeanPath<ItemValue> {

    private static final long serialVersionUID = 932555468L;

    public static final QItemValue itemValue = new QItemValue("itemValue");

    public final NumberPath<Integer> item0 = createNumber("item0", Integer.class);

    public final NumberPath<Integer> item1 = createNumber("item1", Integer.class);

    public final NumberPath<Integer> item2 = createNumber("item2", Integer.class);

    public final NumberPath<Integer> item3 = createNumber("item3", Integer.class);

    public final NumberPath<Integer> item4 = createNumber("item4", Integer.class);

    public final NumberPath<Integer> item5 = createNumber("item5", Integer.class);

    public final NumberPath<Integer> item6 = createNumber("item6", Integer.class);

    public QItemValue(String variable) {
        super(ItemValue.class, forVariable(variable));
    }

    public QItemValue(Path<? extends ItemValue> path) {
        super(path.getType(), path.getMetadata());
    }

    public QItemValue(PathMetadata metadata) {
        super(ItemValue.class, metadata);
    }

}

