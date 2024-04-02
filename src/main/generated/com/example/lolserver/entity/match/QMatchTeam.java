package com.example.lolserver.entity.match;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.example.lolserver.web.match.entity.MatchTeam;
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

    private static final long serialVersionUID = -511733023L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchTeam matchTeam = new QMatchTeam("matchTeam");

    public final BooleanPath baronFirst = createBoolean("baronFirst");

    public final NumberPath<Integer> baronKills = createNumber("baronKills", Integer.class);

    public final BooleanPath championFirst = createBoolean("championFirst");

    public final NumberPath<Integer> championKills = createNumber("championKills", Integer.class);

    public final BooleanPath dragonFirst = createBoolean("dragonFirst");

    public final NumberPath<Integer> dragonKills = createNumber("dragonKills", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath inhibitorFirst = createBoolean("inhibitorFirst");

    public final NumberPath<Integer> inhibitorKills = createNumber("inhibitorKills", Integer.class);

    public final QMatch match;

    public final BooleanPath riftHeraldFirst = createBoolean("riftHeraldFirst");

    public final NumberPath<Integer> riftHeraldKills = createNumber("riftHeraldKills", Integer.class);

    public final NumberPath<Integer> teamId = createNumber("teamId", Integer.class);

    public final BooleanPath towerFirst = createBoolean("towerFirst");

    public final NumberPath<Integer> towerKills = createNumber("towerKills", Integer.class);

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
        this.match = inits.isInitialized("match") ? new QMatch(forProperty("match")) : null;
    }

}

