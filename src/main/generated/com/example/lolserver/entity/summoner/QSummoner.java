package com.example.lolserver.entity.summoner;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSummoner is a Querydsl query type for Summoner
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSummoner extends EntityPathBase<Summoner> {

    private static final long serialVersionUID = -750201290L;

    public static final QSummoner summoner = new QSummoner("summoner");

    public final StringPath accountId = createString("accountId");

    public final StringPath gameName = createString("gameName");

    public final StringPath id = createString("id");

    public final DateTimePath<java.time.LocalDateTime> lastRevisionDateTime = createDateTime("lastRevisionDateTime", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> profileIconId = createNumber("profileIconId", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final NumberPath<Long> revisionDate = createNumber("revisionDate", Long.class);

    public final DateTimePath<java.time.LocalDateTime> revisionDateTime = createDateTime("revisionDateTime", java.time.LocalDateTime.class);

    public final NumberPath<Long> summonerLevel = createNumber("summonerLevel", Long.class);

    public final StringPath tagLine = createString("tagLine");

    public QSummoner(String variable) {
        super(Summoner.class, forVariable(variable));
    }

    public QSummoner(Path<? extends Summoner> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSummoner(PathMetadata metadata) {
        super(Summoner.class, metadata);
    }

}

