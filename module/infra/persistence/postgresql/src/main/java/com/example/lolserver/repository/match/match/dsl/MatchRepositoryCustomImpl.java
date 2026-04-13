package com.example.lolserver.repository.match.match.dsl;

import com.example.lolserver.repository.match.dto.MatchDTO;
import com.example.lolserver.repository.match.dto.MatchSummonerDTO;
import com.example.lolserver.repository.match.dto.QMatchDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.PerkStatValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.PerkStyleValue;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.example.lolserver.support.logging.LogExecutionTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.repository.match.entity.QMatchSummonerEntity.matchSummonerEntity;
import static com.example.lolserver.repository.match.entity.QMatchEntity.matchEntity;
import static com.example.lolserver.repository.match.entity.QMatchTeamEntity.matchTeamEntity;
import static com.example.lolserver.repository.summoner.entity.QSummonerEntity.summonerEntity;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<MatchEntity> getMatches(String puuid, Integer queueId,
            Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<MatchEntity> result = jpaQueryFactory.selectFrom(matchEntity)
                .join(matchSummonerEntity)
                .on(matchSummonerEntity.matchId.eq(matchEntity.matchId))
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId),
                        matchEntity.gameMode.equalsIgnoreCase("CLASSIC")
                                .or(matchEntity.gameMode
                                        .equalsIgnoreCase("CHERRY"))
                )
                .orderBy(matchEntity.gameEndTimestamp.desc())
                .offset((long) pageable.getPageNumber() * pageSize)
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = result.size() > pageSize;
        List<MatchEntity> content = hasNext
                ? result.subList(0, pageSize) : result;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        if (queueId != null) {
            return matchEntity.queueId.eq(queueId);
        }

        return null;
    }

    private BooleanExpression puuidEq(String puuid) {
        return StringUtils.hasText(puuid)
                ? matchSummonerEntity.puuid.eq(puuid) : null;
    }

    private BooleanExpression seasonEq(Integer season) {
        return season != null ? matchEntity.season.eq(season) : null;
    }

    @LogExecutionTime
    @Override
    public Slice<MatchDTO> getMatchDTOs(
            String puuid, Integer season, Integer queueId, Pageable pageable
    ) {
        int pageSize = pageable.getPageSize();

        List<MatchDTO> result = jpaQueryFactory
                .select(new QMatchDTO(
                        matchEntity.matchId,
                        matchEntity.dataVersion,
                        matchEntity.gameCreation,
                        matchEntity.gameDuration,
                        matchEntity.gameEndTimestamp,
                        matchEntity.gameStartTimestamp,
                        matchEntity.gameMode,
                        matchEntity.gameType,
                        matchEntity.gameVersion,
                        matchEntity.mapId,
                        matchEntity.queueId,
                        matchEntity.platformId,
                        matchEntity.tournamentCode,
                        matchEntity.averageTier
                ))
                .from(matchEntity)
                .join(matchSummonerEntity)
                .on(matchSummonerEntity.matchId.eq(matchEntity.matchId))
                .where(
                        puuidEq(puuid),
                        seasonEq(season),
                        queueIdEq(queueId),
                        matchEntity.gameMode.equalsIgnoreCase("CLASSIC")
                                .or(matchEntity.gameMode
                                        .equalsIgnoreCase("CHERRY"))
                )
                .orderBy(matchEntity.gameEndTimestamp.desc())
                .offset((long) pageable.getPageNumber() * pageSize)
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = result.size() > pageSize;
        List<MatchDTO> content = hasNext
                ? result.subList(0, pageSize) : result;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @LogExecutionTime
    @Override
    public List<MatchSummonerDTO> getMatchSummoners(
            List<String> matchIds
    ) {
        return jpaQueryFactory
                .select(matchSummonerProjection())
                .from(matchSummonerEntity)
                .leftJoin(summonerEntity)
                    .on(summonerEntity.puuid.eq(
                            matchSummonerEntity.puuid))
                .leftJoin(matchTeamEntity)
                    .on(matchTeamEntity.matchId.eq(
                            matchSummonerEntity.matchId)
                        .and(matchTeamEntity.teamId.eq(
                            matchSummonerEntity.teamId)))
                .where(matchSummonerEntity.matchId.in(matchIds))
                .fetch();
    }

    @Override
    public List<MatchSummonerDTO> getMatchSummoners(String matchId) {
        return getMatchSummoners(List.of(matchId));
    }

    private Expression<MatchSummonerDTO> matchSummonerProjection() {
        return Projections.bean(MatchSummonerDTO.class,
                matchSummonerEntity.puuid,
                matchSummonerEntity.matchId,
                matchSummonerEntity.summonerId,
                gameNameCoalesce(), tagLineCoalesce(),
                matchSummonerEntity.profileIcon,
                matchSummonerEntity.participantId,
                matchSummonerEntity.tier,
                matchSummonerEntity.tierRank,
                matchSummonerEntity.absolutePoints,
                matchSummonerEntity.summonerLevel,
                matchSummonerEntity.champLevel,
                matchSummonerEntity.championId,
                matchSummonerEntity.championName,
                matchSummonerEntity.champExperience,
                matchSummonerEntity.summoner1Id,
                matchSummonerEntity.summoner2Id,
                matchSummonerEntity.kills,
                matchSummonerEntity.assists,
                matchSummonerEntity.deaths,
                matchSummonerEntity.doubleKills,
                matchSummonerEntity.tripleKills,
                matchSummonerEntity.quadraKills,
                matchSummonerEntity.pentaKills,
                matchSummonerEntity.goldEarned,
                matchSummonerEntity.consumablesPurchased,
                matchSummonerEntity.itemsPurchased,
                matchSummonerEntity.neutralMinionsKilled,
                matchSummonerEntity.totalMinionsKilled,
                matchSummonerEntity.visionScore,
                matchSummonerEntity.visionWardsBoughtInGame,
                matchSummonerEntity.wardsKilled,
                matchSummonerEntity.wardsPlaced,
                matchSummonerEntity.totalDamageDealtToChampions,
                matchSummonerEntity.totalDamageTaken,
                matchSummonerEntity.teamId,
                matchSummonerEntity.teamPosition,
                matchSummonerEntity.win,
                matchSummonerEntity.timePlayed,
                matchSummonerEntity.timeCCingOthers,
                matchSummonerEntity.individualPosition,
                matchSummonerEntity.lane,
                matchSummonerEntity.role,
                matchSummonerEntity.placement,
                matchSummonerEntity.playerAugment1,
                matchSummonerEntity.playerAugment2,
                matchSummonerEntity.playerAugment3,
                matchSummonerEntity.playerAugment4,
                itemProjection(),
                perkStatProjection(),
                perkStyleProjection(),
                matchTeamEntity.championKills.as("teamChampionKills"),
                matchTeamEntity.baronKills.as("teamBaronKills"),
                matchTeamEntity.dragonKills.as("teamDragonKills"),
                matchTeamEntity.towerKills.as("teamTowerKills"),
                matchTeamEntity.inhibitorKills.as("teamInhibitorKills")
        );
    }

    private Expression<String> gameNameCoalesce() {
        return Expressions.stringTemplate(
                "COALESCE({0}, {1})",
                summonerEntity.gameName,
                matchSummonerEntity.riotIdGameName)
                .as("riotIdGameName");
    }

    private Expression<String> tagLineCoalesce() {
        return Expressions.stringTemplate(
                "COALESCE({0}, {1})",
                summonerEntity.tagLine,
                matchSummonerEntity.riotIdTagline)
                .as("riotIdTagline");
    }

    private Expression<ItemValue> itemProjection() {
        return Projections.bean(ItemValue.class,
                matchSummonerEntity.item.item0,
                matchSummonerEntity.item.item1,
                matchSummonerEntity.item.item2,
                matchSummonerEntity.item.item3,
                matchSummonerEntity.item.item4,
                matchSummonerEntity.item.item5,
                matchSummonerEntity.item.item6
        ).as("item");
    }

    private Expression<PerkStatValue>
            perkStatProjection() {
        return Projections.bean(PerkStatValue.class,
                matchSummonerEntity.perkStat.statPerkDefense,
                matchSummonerEntity.perkStat.statPerkFlex,
                matchSummonerEntity.perkStat.statPerkOffense
        ).as("perkStat");
    }

    private Expression<PerkStyleValue>
            perkStyleProjection() {
        return Projections.bean(PerkStyleValue.class,
                matchSummonerEntity.perkStyle.primaryStyleId,
                matchSummonerEntity.perkStyle.primaryPerk0,
                matchSummonerEntity.perkStyle.primaryPerk1,
                matchSummonerEntity.perkStyle.primaryPerk2,
                matchSummonerEntity.perkStyle.primaryPerk3,
                matchSummonerEntity.perkStyle.subStyleId,
                matchSummonerEntity.perkStyle.subPerk0,
                matchSummonerEntity.perkStyle.subPerk1
        ).as("perkStyle");
    }

}
