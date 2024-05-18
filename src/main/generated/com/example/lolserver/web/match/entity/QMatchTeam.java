package com.example.lolserver.web.match.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatchTeam is a Querydsl query type for MatchTeam
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchTeam extends EntityPathBase<MatchTeam> {

    private static final long serialVersionUID = -341012755L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchTeam matchTeam = new QMatchTeam("matchTeam");

    public final com.example.lolserver.web.match.entity.id.QMatchTeamId id;

    public final QMatch match;

    public final com.example.lolserver.web.match.entity.value.team.QTeamBanValue teamBan;

    public final com.example.lolserver.web.match.entity.value.team.QTeamObjectValue teamObject;

    public final BooleanPath win = createBoolean("win");

    public QMatchTeam(String variable) {
        this(MatchTeam.class, forVariable(variable), INITS);
    }

    public QMatchTeam(Path<? extends MatchTeam> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMatchTeam(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMatchTeam(PathMetadata metadata, PathInits inits) {
        this(MatchTeam.class, metadata, inits);
    }

    public QMatchTeam(Class<? extends MatchTeam> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new com.example.lolserver.web.match.entity.id.QMatchTeamId(forProperty("id")) : null;
        this.match = inits.isInitialized("match") ? new QMatch(forProperty("match")) : null;
        this.teamBan = inits.isInitialized("teamBan") ? new com.example.lolserver.web.match.entity.value.team.QTeamBanValue(forProperty("teamBan")) : null;
        this.teamObject = inits.isInitialized("teamObject") ? new com.example.lolserver.web.match.entity.value.team.QTeamObjectValue(forProperty("teamObject")) : null;
    }

}

