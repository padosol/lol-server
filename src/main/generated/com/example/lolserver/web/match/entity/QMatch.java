package com.example.lolserver.web.match.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMatch is a Querydsl query type for Match
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatch extends EntityPathBase<Match> {

    private static final long serialVersionUID = 1495437424L;

    public static final QMatch match = new QMatch("match");

    public final StringPath dateVersion = createString("dateVersion");

    public final StringPath endOfGameResult = createString("endOfGameResult");

    public final NumberPath<Long> gameCreation = createNumber("gameCreation", Long.class);

    public final NumberPath<Long> gameDuration = createNumber("gameDuration", Long.class);

    public final NumberPath<Long> gameEndTimestamp = createNumber("gameEndTimestamp", Long.class);

    public final NumberPath<Long> gameId = createNumber("gameId", Long.class);

    public final StringPath gameMode = createString("gameMode");

    public final StringPath gameName = createString("gameName");

    public final NumberPath<Long> gameStartTimestamp = createNumber("gameStartTimestamp", Long.class);

    public final StringPath gameType = createString("gameType");

    public final StringPath gameVersion = createString("gameVersion");

    public final NumberPath<Integer> mapId = createNumber("mapId", Integer.class);

    public final StringPath matchId = createString("matchId");

    public final StringPath platformId = createString("platformId");

    public final NumberPath<Integer> queueId = createNumber("queueId", Integer.class);

    public final NumberPath<Integer> season = createNumber("season", Integer.class);

    public final StringPath tournamentCode = createString("tournamentCode");

    public QMatch(String variable) {
        super(Match.class, forVariable(variable));
    }

    public QMatch(Path<? extends Match> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMatch(PathMetadata metadata) {
        super(Match.class, metadata);
    }

}

