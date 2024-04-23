package com.example.lolserver.web.match.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatchTeamBan is a Querydsl query type for MatchTeamBan
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchTeamBan extends EntityPathBase<MatchTeamBan> {

    private static final long serialVersionUID = -1513262622L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchTeamBan matchTeamBan = new QMatchTeamBan("matchTeamBan");

    public final NumberPath<Integer> championId = createNumber("championId", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMatchTeam matchTeam;

    public final NumberPath<Integer> pickTurn = createNumber("pickTurn", Integer.class);

    public QMatchTeamBan(String variable) {
        this(MatchTeamBan.class, forVariable(variable), INITS);
    }

    public QMatchTeamBan(Path<? extends MatchTeamBan> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMatchTeamBan(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMatchTeamBan(PathMetadata metadata, PathInits inits) {
        this(MatchTeamBan.class, metadata, inits);
    }

    public QMatchTeamBan(Class<? extends MatchTeamBan> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.matchTeam = inits.isInitialized("matchTeam") ? new QMatchTeam(forProperty("matchTeam"), inits.get("matchTeam")) : null;
    }

}

