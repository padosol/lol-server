package com.example.lolserver.web.match.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatchSummoner is a Querydsl query type for MatchSummoner
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchSummoner extends EntityPathBase<MatchSummoner> {

    private static final long serialVersionUID = 250748382L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchSummoner matchSummoner = new QMatchSummoner("matchSummoner");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final NumberPath<Integer> baronKills = createNumber("baronKills", Integer.class);

    public final NumberPath<Integer> bountyLevel = createNumber("bountyLevel", Integer.class);

    public final NumberPath<Integer> champExperience = createNumber("champExperience", Integer.class);

    public final NumberPath<Integer> championId = createNumber("championId", Integer.class);

    public final StringPath championName = createString("championName");

    public final NumberPath<Integer> championTransform = createNumber("championTransform", Integer.class);

    public final NumberPath<Integer> champLevel = createNumber("champLevel", Integer.class);

    public final NumberPath<Integer> consumablesPurchased = createNumber("consumablesPurchased", Integer.class);

    public final NumberPath<Integer> damageDealtToBuildings = createNumber("damageDealtToBuildings", Integer.class);

    public final NumberPath<Integer> damageDealtToObjectives = createNumber("damageDealtToObjectives", Integer.class);

    public final NumberPath<Integer> damageDealtToTurrets = createNumber("damageDealtToTurrets", Integer.class);

    public final NumberPath<Integer> damageSelfMitigated = createNumber("damageSelfMitigated", Integer.class);

    public final NumberPath<Integer> deaths = createNumber("deaths", Integer.class);

    public final NumberPath<Integer> detectorWardsPlaced = createNumber("detectorWardsPlaced", Integer.class);

    public final NumberPath<Integer> doubleKills = createNumber("doubleKills", Integer.class);

    public final NumberPath<Integer> dragonKills = createNumber("dragonKills", Integer.class);

    public final BooleanPath firstBloodAssist = createBoolean("firstBloodAssist");

    public final BooleanPath firstBloodKill = createBoolean("firstBloodKill");

    public final BooleanPath firstTowerAssist = createBoolean("firstTowerAssist");

    public final BooleanPath firstTowerKill = createBoolean("firstTowerKill");

    public final BooleanPath gameEndedInEarlySurrender = createBoolean("gameEndedInEarlySurrender");

    public final BooleanPath gameEndedInSurrender = createBoolean("gameEndedInSurrender");

    public final NumberPath<Integer> goldEarned = createNumber("goldEarned", Integer.class);

    public final NumberPath<Integer> goldSpent = createNumber("goldSpent", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath individualPosition = createString("individualPosition");

    public final NumberPath<Integer> inhibitorKills = createNumber("inhibitorKills", Integer.class);

    public final NumberPath<Integer> inhibitorsLost = createNumber("inhibitorsLost", Integer.class);

    public final NumberPath<Integer> inhibitorTakedowns = createNumber("inhibitorTakedowns", Integer.class);

    public final com.example.lolserver.web.match.entity.value.QItemValue item;

    public final NumberPath<Integer> itemsPurchased = createNumber("itemsPurchased", Integer.class);

    public final NumberPath<Integer> killingSprees = createNumber("killingSprees", Integer.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final StringPath lane = createString("lane");

    public final NumberPath<Integer> largestCriticalStrike = createNumber("largestCriticalStrike", Integer.class);

    public final NumberPath<Integer> largestKillingSpree = createNumber("largestKillingSpree", Integer.class);

    public final NumberPath<Integer> largestMultiKill = createNumber("largestMultiKill", Integer.class);

    public final NumberPath<Integer> longestTimeSpentLiving = createNumber("longestTimeSpentLiving", Integer.class);

    public final NumberPath<Integer> magicDamageDealt = createNumber("magicDamageDealt", Integer.class);

    public final NumberPath<Integer> magicDamageDealtToChampions = createNumber("magicDamageDealtToChampions", Integer.class);

    public final NumberPath<Integer> magicDamageTaken = createNumber("magicDamageTaken", Integer.class);

    public final QMatch match;

    public final NumberPath<Integer> neutralMinionsKilled = createNumber("neutralMinionsKilled", Integer.class);

    public final NumberPath<Integer> nexusKills = createNumber("nexusKills", Integer.class);

    public final NumberPath<Integer> nexusLost = createNumber("nexusLost", Integer.class);

    public final NumberPath<Integer> nexusTakedowns = createNumber("nexusTakedowns", Integer.class);

    public final NumberPath<Integer> objectivesStolen = createNumber("objectivesStolen", Integer.class);

    public final NumberPath<Integer> objectivesStolenAssists = createNumber("objectivesStolenAssists", Integer.class);

    public final NumberPath<Integer> participantId = createNumber("participantId", Integer.class);

    public final NumberPath<Integer> pentaKills = createNumber("pentaKills", Integer.class);

    public final NumberPath<Integer> physicalDamageDealt = createNumber("physicalDamageDealt", Integer.class);

    public final NumberPath<Integer> physicalDamageDealtToChampions = createNumber("physicalDamageDealtToChampions", Integer.class);

    public final NumberPath<Integer> physicalDamageTaken = createNumber("physicalDamageTaken", Integer.class);

    public final NumberPath<Integer> profileIcon = createNumber("profileIcon", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final NumberPath<Integer> quadraKills = createNumber("quadraKills", Integer.class);

    public final StringPath riotIdGameName = createString("riotIdGameName");

    public final StringPath riotIdTagline = createString("riotIdTagline");

    public final StringPath role = createString("role");

    public final NumberPath<Integer> sightWardsBoughtInGame = createNumber("sightWardsBoughtInGame", Integer.class);

    public final NumberPath<Integer> spell1Casts = createNumber("spell1Casts", Integer.class);

    public final NumberPath<Integer> spell2Casts = createNumber("spell2Casts", Integer.class);

    public final NumberPath<Integer> spell3Casts = createNumber("spell3Casts", Integer.class);

    public final NumberPath<Integer> spell4Casts = createNumber("spell4Casts", Integer.class);

    public final com.example.lolserver.web.match.entity.value.QStatValue statValue;

    public final com.example.lolserver.web.match.entity.value.QStyleValue styleValue;

    public final NumberPath<Integer> summoner1Casts = createNumber("summoner1Casts", Integer.class);

    public final NumberPath<Integer> summoner1Id = createNumber("summoner1Id", Integer.class);

    public final NumberPath<Integer> summoner2Casts = createNumber("summoner2Casts", Integer.class);

    public final NumberPath<Integer> summoner2Id = createNumber("summoner2Id", Integer.class);

    public final StringPath summonerId = createString("summonerId");

    public final NumberPath<Integer> summonerLevel = createNumber("summonerLevel", Integer.class);

    public final StringPath summonerName = createString("summonerName");

    public final BooleanPath teamEarlySurrendered = createBoolean("teamEarlySurrendered");

    public final NumberPath<Integer> teamId = createNumber("teamId", Integer.class);

    public final StringPath teamPosition = createString("teamPosition");

    public final NumberPath<Integer> timeCCingOthers = createNumber("timeCCingOthers", Integer.class);

    public final NumberPath<Integer> timePlayed = createNumber("timePlayed", Integer.class);

    public final NumberPath<Integer> totalDamageDealt = createNumber("totalDamageDealt", Integer.class);

    public final NumberPath<Integer> totalDamageDealtToChampions = createNumber("totalDamageDealtToChampions", Integer.class);

    public final NumberPath<Integer> totalDamageShieldedOnTeammates = createNumber("totalDamageShieldedOnTeammates", Integer.class);

    public final NumberPath<Integer> totalDamageTaken = createNumber("totalDamageTaken", Integer.class);

    public final NumberPath<Integer> totalHeal = createNumber("totalHeal", Integer.class);

    public final NumberPath<Integer> totalHealsOnTeammates = createNumber("totalHealsOnTeammates", Integer.class);

    public final NumberPath<Integer> totalMinionsKilled = createNumber("totalMinionsKilled", Integer.class);

    public final NumberPath<Integer> totalTimeCCDealt = createNumber("totalTimeCCDealt", Integer.class);

    public final NumberPath<Integer> totalTimeSpentDead = createNumber("totalTimeSpentDead", Integer.class);

    public final NumberPath<Integer> totalUnitsHealed = createNumber("totalUnitsHealed", Integer.class);

    public final NumberPath<Integer> tripleKills = createNumber("tripleKills", Integer.class);

    public final NumberPath<Integer> trueDamageDealt = createNumber("trueDamageDealt", Integer.class);

    public final NumberPath<Integer> trueDamageDealtToChampions = createNumber("trueDamageDealtToChampions", Integer.class);

    public final NumberPath<Integer> trueDamageTaken = createNumber("trueDamageTaken", Integer.class);

    public final NumberPath<Integer> turretKills = createNumber("turretKills", Integer.class);

    public final NumberPath<Integer> turretsLost = createNumber("turretsLost", Integer.class);

    public final NumberPath<Integer> turretTakedowns = createNumber("turretTakedowns", Integer.class);

    public final NumberPath<Integer> unrealKills = createNumber("unrealKills", Integer.class);

    public final NumberPath<Integer> visionScore = createNumber("visionScore", Integer.class);

    public final NumberPath<Integer> visionWardsBoughtInGame = createNumber("visionWardsBoughtInGame", Integer.class);

    public final NumberPath<Integer> wardsKilled = createNumber("wardsKilled", Integer.class);

    public final NumberPath<Integer> wardsPlaced = createNumber("wardsPlaced", Integer.class);

    public final BooleanPath win = createBoolean("win");

    public QMatchSummoner(String variable) {
        this(MatchSummoner.class, forVariable(variable), INITS);
    }

    public QMatchSummoner(Path<? extends MatchSummoner> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMatchSummoner(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMatchSummoner(PathMetadata metadata, PathInits inits) {
        this(MatchSummoner.class, metadata, inits);
    }

    public QMatchSummoner(Class<? extends MatchSummoner> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.item = inits.isInitialized("item") ? new com.example.lolserver.web.match.entity.value.QItemValue(forProperty("item")) : null;
        this.match = inits.isInitialized("match") ? new QMatch(forProperty("match")) : null;
        this.statValue = inits.isInitialized("statValue") ? new com.example.lolserver.web.match.entity.value.QStatValue(forProperty("statValue")) : null;
        this.styleValue = inits.isInitialized("styleValue") ? new com.example.lolserver.web.match.entity.value.QStyleValue(forProperty("styleValue")) : null;
    }

}

