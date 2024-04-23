package com.example.lolserver.web.league.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLeague is a Querydsl query type for League
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLeague extends EntityPathBase<League> {

    private static final long serialVersionUID = 612293896L;

    public static final QLeague league = new QLeague("league");

    public final StringPath leagueId = createString("leagueId");

    public final StringPath name = createString("name");

    public final EnumPath<QueueType> queue = createEnum("queue", QueueType.class);

    public final StringPath tier = createString("tier");

    public QLeague(String variable) {
        super(League.class, forVariable(variable));
    }

    public QLeague(Path<? extends League> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLeague(PathMetadata metadata) {
        super(League.class, metadata);
    }

}

