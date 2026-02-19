package com.example.lolserver.repository.match.matchsummoner.dsl.impl;

import com.example.lolserver.repository.match.dto.LinePosition;
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.dto.QMSChampionDTO;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.repository.match.entity.QMatchEntity.matchEntity;
import static com.example.lolserver.repository.match.entity.QMatchSummonerEntity.matchSummonerEntity;
import static com.example.lolserver.repository.match.entity.QChallengesEntity.challengesEntity;

@Repository
@RequiredArgsConstructor
public class MatchSummonerRepositoryCustomImpl implements MatchSummonerRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public Page<MatchSummonerEntity> findAllByPuuidAndQueueId(String puuid, Integer queueId, Pageable pageable) {
        List<MatchSummonerEntity> content = jpaQueryFactory.selectFrom(matchSummonerEntity)
                .join(matchEntity).on(matchEntity.matchId.eq(matchSummonerEntity.matchId))
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId)
                )
                .orderBy(matchSummonerEntity.matchId.desc())
                .offset((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> count = jpaQueryFactory.select(matchSummonerEntity.count())
                .from(matchSummonerEntity)
                .join(matchEntity).on(matchEntity.matchId.eq(matchSummonerEntity.matchId))
                .where(
                        puuidEq(puuid)
                );


        return PageableExecutionUtils.getPage(content, pageable, count::fetchOne);
    }

    @Override
    public List<String> findAllByMatchIdNotExist(List<String> matchIds) {

        List<String> inData = jpaQueryFactory.select(matchEntity.matchId)
                .from(matchEntity)
                .where(matchEntity.matchId.in(matchIds))
                .fetch();

        return matchIds.stream().filter( matchId -> !inData.contains(matchId)).toList();
    }

    @Override
    public List<MSChampionDTO> findAllChampionKDAByPuuidAndSeasonAndQueueType(
            String puuid, Integer season) {

        JPAQuery<MSChampionDTO> query = jpaQueryFactory.select(
                        Projections.fields(MSChampionDTO.class,
                                matchSummonerEntity.championId,
                                matchSummonerEntity.championName,
                                Expressions.template(Double.class, "ROUND({0}, 1)",
                                        matchSummonerEntity.kills.avg()).as("kills"),
                                Expressions.template(Double.class, "ROUND({0}, 1)",
                                        matchSummonerEntity.deaths.avg()).as("deaths"),
                                Expressions.template(Double.class, "ROUND({0}, 1)",
                                        matchSummonerEntity.assists.avg()).as("assists"),
                                Expressions.template(Double.class, "ROUND({0}, 1)",
                                        matchSummonerEntity.assists.avg()).as("assists"),
                                Expressions.template(Double.class, "ROUND({0}, 1)",
                                        matchSummonerEntity.neutralMinionsKilled
                                                .add(matchSummonerEntity.totalMinionsKilled)
                                                .avg()).as("cs"),
                                Expressions.template(Double.class, "ROUND({0}, 1)",
                                        matchEntity.gameDuration.avg()).as("duration"),
                                new CaseBuilder()
                                        .when(matchSummonerEntity.win.isTrue())
                                        .then(1L).otherwise(0L).sum().as("win"),
                                matchSummonerEntity.count().as("playCount")
                        )
                ).from(matchSummonerEntity)
                .join(matchEntity).on(matchEntity.matchId.eq(matchSummonerEntity.matchId))
                .where(
                        matchSummonerEntity.puuid.eq(puuid),
                        matchEntity.season.eq(season),
                        matchSummonerEntity.gameEndedInEarlySurrender.eq(false)
                )
                .groupBy(matchSummonerEntity.championId, matchSummonerEntity.championName)
                .orderBy(Expressions.stringPath("playCount").desc());

        return query.fetch();
    }

    @Override
    public List<MSChampionDTO> findAllMatchSummonerByPuuidAndSeason(String puuid, Integer season, Integer queueId) {
        // 1. 승리/패배 횟수를 계산할 CASE-SUM Expression 정의
        NumberExpression<Long> winCountExpr = new CaseBuilder()
                .when(matchSummonerEntity.win.isTrue())
                .then(1L)
                .otherwise(0L)
                .sum(); // Long 타입으로 합계

        NumberExpression<Long> lossCountExpr = new CaseBuilder()
                .when(matchSummonerEntity.win.isFalse())
                .then(1L)
                .otherwise(0L)
                .sum(); // Long 타입으로 합계

        QMSChampionDTO qmsChampionDTO = new QMSChampionDTO(
            matchSummonerEntity.assists.avg(),
            matchSummonerEntity.deaths.avg(),
            matchSummonerEntity.kills.avg(),
            matchSummonerEntity.championId,
            matchSummonerEntity.championName,
                winCountExpr,
                lossCountExpr,
            challengesEntity.damagePerMinute.avg(),
            challengesEntity.kda.avg(),
            challengesEntity.laneMinionsFirst10Minutes.avg(),
            challengesEntity.damageTakenOnTeamPercentage.avg(),
            challengesEntity.goldPerMinute.avg(),
            matchSummonerEntity.championId.count()
        );

        JPAQuery<MSChampionDTO> query = jpaQueryFactory.select(qmsChampionDTO)
                .from(matchSummonerEntity)
                .join(matchEntity)
                .on(matchEntity.matchId.eq(matchSummonerEntity.matchId))
                .join(challengesEntity)
                .on(
                        challengesEntity.puuid
                                .eq(matchSummonerEntity.puuid)
                                .and(challengesEntity.matchId
                                        .eq(matchSummonerEntity.matchId))
                )
                .where(
                        puuidEq(puuid),
                        seasonEq(season),
                        queueIdEq(queueId)
                )
                .groupBy(matchSummonerEntity.championId, matchSummonerEntity.championName)
                .orderBy(matchSummonerEntity.championId.count().desc());

        return query.fetch();
    }

    @Override
    public List<LinePosition> findAllPositionByPuuidAndLimit(String puuid, Long limit) {

        JPAQuery<LinePosition> query = jpaQueryFactory.select(
                        Projections.fields(
                                LinePosition.class,
                                matchSummonerEntity.individualPosition.as("position"),
                                matchSummonerEntity.individualPosition.count().as("playCount")
                        )
                )
                .from(matchSummonerEntity)
                .where(matchSummonerEntity.puuid.eq(puuid))
                .orderBy(Expressions.stringPath("playCount").desc())
                .groupBy(matchSummonerEntity.individualPosition);

        if (limit != null) {
            query.limit(limit);
        }

        return query.fetch();
    }

    @Override
    public Slice<String> findAllMatchIdsByPuuidWithPage(String puuid, Integer queueId, Pageable pageable) {
        List<String> matchIds = jpaQueryFactory.select(matchSummonerEntity.matchId).from(matchSummonerEntity)
                .join(matchEntity).on(matchEntity.matchId.eq(matchSummonerEntity.matchId))
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId)
                )
                .orderBy(matchSummonerEntity.matchId.desc())
                .offset((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> count = jpaQueryFactory.select(matchSummonerEntity.count())
                .from(matchSummonerEntity)
                .join(matchEntity).on(matchEntity.matchId.eq(matchSummonerEntity.matchId))
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId)
                );


        return PageableExecutionUtils.getPage(
                matchIds, pageable, count::fetchOne);
    }

    private BooleanExpression queueIdEqOrAll(Integer queueId) {
        return queueId == null
                ? matchEntity.queueId.eq(420)
                    .or(matchEntity.queueId.eq(440))
                : matchEntity.queueId.eq(queueId);
    }

    private BooleanExpression puuidEq(String puuid) {
        return StringUtils.hasText(puuid) ? matchSummonerEntity.puuid.eq(puuid) : null;
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        return queueId != null ? matchEntity.queueId.eq(queueId) : null;
    }

    private BooleanExpression seasonEq(Integer season) {
        return matchEntity.season.eq(season);
    }
}
