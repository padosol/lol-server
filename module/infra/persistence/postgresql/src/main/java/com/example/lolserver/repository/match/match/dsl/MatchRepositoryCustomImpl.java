package com.example.lolserver.repository.match.match.dsl;

import com.example.lolserver.repository.match.dto.MatchDTO;
import com.example.lolserver.repository.match.dto.MatchSummonerDTO;
import com.example.lolserver.repository.match.dto.MatchTeamDTO;
import com.example.lolserver.repository.match.dto.QMatchDTO;
import com.example.lolserver.repository.match.dto.QMatchTeamDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StatValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StyleValue;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
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

@Slf4j
@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<MatchEntity> getMatches(String puuid, Integer queueId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<MatchEntity> result = jpaQueryFactory.selectFrom(matchEntity)
                .join(matchSummonerEntity)
                .on(matchSummonerEntity.matchId.eq(matchEntity.matchId))
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId),
                        matchEntity.gameMode.equalsIgnoreCase("CLASSIC")
                                .or(matchEntity.gameMode.equalsIgnoreCase("CHERRY"))
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
        return StringUtils.hasText(puuid) ? matchSummonerEntity.puuid.eq(puuid) : null;
    }

    @Override
    public Slice<MatchDTO> getMatchDTOs(
            String puuid, Integer queueId, Pageable pageable
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

    @Override
    public List<MatchSummonerDTO> getMatchSummoners(
            List<String> matchIds
    ) {
        return jpaQueryFactory
                .select(Projections.bean(MatchSummonerDTO.class,
                        matchSummonerEntity.puuid,
                        matchSummonerEntity.matchId,
                        matchSummonerEntity.summonerId,
                        matchSummonerEntity.riotIdGameName,
                        matchSummonerEntity.riotIdTagline,
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
                        statValueProjection(),
                        styleValueProjection()
                ))
                .from(matchSummonerEntity)
                .where(matchSummonerEntity.matchId.in(matchIds))
                .fetch();
    }

    private com.querydsl.core.types.Expression<ItemValue> itemProjection() {
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

    private com.querydsl.core.types.Expression<StatValue> statValueProjection() {
        return Projections.bean(StatValue.class,
                matchSummonerEntity.statValue.defense,
                matchSummonerEntity.statValue.flex,
                matchSummonerEntity.statValue.offense
        ).as("statValue");
    }

    private com.querydsl.core.types.Expression<StyleValue> styleValueProjection() {
        return Projections.bean(StyleValue.class,
                matchSummonerEntity.styleValue.primaryRuneId,
                matchSummonerEntity.styleValue.primaryRuneIds,
                matchSummonerEntity.styleValue.secondaryRuneId,
                matchSummonerEntity.styleValue.secondaryRuneIds
        ).as("styleValue");
    }

    @Override
    public List<MatchTeamDTO> getMatchTeams(List<String> matchIds) {
        return jpaQueryFactory
                .select(new QMatchTeamDTO(
                        matchTeamEntity.matchId,
                        matchTeamEntity.teamId,
                        matchTeamEntity.win,
                        matchTeamEntity.championKills,
                        matchTeamEntity.baronKills,
                        matchTeamEntity.dragonKills,
                        matchTeamEntity.towerKills,
                        matchTeamEntity.inhibitorKills,
                        matchTeamEntity.champion1Id,
                        matchTeamEntity.champion2Id,
                        matchTeamEntity.champion3Id,
                        matchTeamEntity.champion4Id,
                        matchTeamEntity.champion5Id,
                        matchTeamEntity.pick1Turn,
                        matchTeamEntity.pick2Turn,
                        matchTeamEntity.pick3Turn,
                        matchTeamEntity.pick4Turn,
                        matchTeamEntity.pick5Turn
                ))
                .from(matchTeamEntity)
                .where(matchTeamEntity.matchId.in(matchIds))
                .fetch();
    }
}
