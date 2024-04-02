package com.example.lolserver.entity.league;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLeagueSummoner is a Querydsl query type for LeagueSummoner
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLeagueSummoner extends EntityPathBase<LeagueSummoner> {

    private static final long serialVersionUID = 1908429444L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLeagueSummoner leagueSummoner = new QLeagueSummoner("leagueSummoner");

    public final BooleanPath freshBlood = createBoolean("freshBlood");

    public final BooleanPath hotStreak = createBoolean("hotStreak");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath inactive = createBoolean("inactive");

    public final QLeague league;

    public final NumberPath<Integer> leaguePoints = createNumber("leaguePoints", Integer.class);

    public final NumberPath<Integer> losses = createNumber("losses", Integer.class);

    public final StringPath rank = createString("rank");

    public final com.example.lolserver.web.summoner.entity.QSummoner summoner;

    public final BooleanPath veteran = createBoolean("veteran");

    public final NumberPath<Integer> wins = createNumber("wins", Integer.class);

    public QLeagueSummoner(String variable) {
        this(LeagueSummoner.class, forVariable(variable), INITS);
    }

    public QLeagueSummoner(Path<? extends LeagueSummoner> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLeagueSummoner(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLeagueSummoner(PathMetadata metadata, PathInits inits) {
        this(LeagueSummoner.class, metadata, inits);
    }

    public QLeagueSummoner(Class<? extends LeagueSummoner> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.league = inits.isInitialized("league") ? new QLeague(forProperty("league")) : null;
        this.summoner = inits.isInitialized("summoner") ? new com.example.lolserver.web.summoner.entity.QSummoner(forProperty("summoner")) : null;
    }

}

