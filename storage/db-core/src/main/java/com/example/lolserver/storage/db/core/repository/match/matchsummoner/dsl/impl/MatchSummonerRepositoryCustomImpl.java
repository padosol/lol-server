package com.example.lolserver.storage.db.core.repository.match.matchsummoner.dsl.impl;

import com.example.lolserver.storage.db.core.repository.match.dto.LinePosition;
import com.example.lolserver.storage.db.core.repository.match.dto.MSChampionDTO;
import com.example.lolserver.storage.db.core.repository.match.dto.QMSChampionDTO;
import com.example.lolserver.storage.db.core.repository.match.entity.MatchSummoner;
import com.example.lolserver.storage.db.core.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.storage.db.core.repository.match.entity.QMatch.match;
import static com.example.lolserver.storage.db.core.repository.match.entity.QMatchSummoner.matchSummoner;
import static com.example.lolserver.storage.db.core.repository.match.entity.QChallenges.challenges;

@Repository
@RequiredArgsConstructor
public class MatchSummonerRepositoryCustomImpl implements MatchSummonerRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public Page<MatchSummoner> findAllByPuuidAndQueueId(String puuid, Integer queueId, Pageable pageable) {
        List<MatchSummoner> content = jpaQueryFactory.selectFrom(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId)
                )
                .orderBy(matchSummoner.match.matchId.desc())
                .offset((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> count = jpaQueryFactory.select(matchSummoner.count())
                .from(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        puuidEq(puuid)
                );


        return PageableExecutionUtils.getPage(content, pageable, count::fetchOne);
    }

    @Override
    public List<String> findAllByMatchIdNotExist(List<String> matchIds) {

        List<String> inData = jpaQueryFactory.select(match.matchId)
                .from(match)
                .where(match.matchId.in(matchIds))
                .fetch();

        return matchIds.stream().filter( matchId -> !inData.contains(matchId)).toList();
    }

    @Override
    public List<MSChampionDTO> findAllChampionKDAByPuuidAndSeasonAndQueueType(
            String puuid, Integer season) {

        JPAQuery<MSChampionDTO> query = jpaQueryFactory.select(
                        Projections.fields(MSChampionDTO.class,
                                matchSummoner.championId,
                                matchSummoner.championName,
                                Expressions.template(Double.class, "ROUND({0}, 1)", matchSummoner.kills.avg()).as("kills"),
                                Expressions.template(Double.class, "ROUND({0}, 1)", matchSummoner.deaths.avg()).as("deaths"),
                                Expressions.template(Double.class, "ROUND({0}, 1)", matchSummoner.assists.avg()).as("assists"),
                                Expressions.template(Double.class, "ROUND({0}, 1)", matchSummoner.assists.avg()).as("assists"),
                                Expressions.template(Double.class, "ROUND({0}, 1)", matchSummoner.neutralMinionsKilled.add(matchSummoner.totalMinionsKilled).avg()).as("cs"),
                                Expressions.template(Double.class, "ROUND({0}, 1)", match.gameDuration.avg()).as("duration"),
                                new CaseBuilder().when(matchSummoner.win.isTrue()).then(1L).otherwise(0L).sum().as("win"),
                                matchSummoner.count().as("playCount")
                        )
                ).from(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        matchSummoner.puuid.eq(puuid),
                        match.season.eq(season),
                        matchSummoner.gameEndedInEarlySurrender.eq(false)
                )
                .groupBy(matchSummoner.championId, matchSummoner.championName)
                .orderBy(Expressions.stringPath("playCount").desc());

        return query.fetch();
    }

    @Override
    public List<MSChampionDTO> findAllMatchSummonerByPuuidAndSeason(String puuid, Integer season) {
        // 1. 승리/패배 횟수를 계산할 CASE-SUM Expression 정의
        NumberExpression<Long> winCountExpr = new CaseBuilder()
                .when(matchSummoner.win.isTrue())
                .then(1L)
                .otherwise(0L)
                .sum(); // Long 타입으로 합계

        NumberExpression<Long> lossCountExpr = new CaseBuilder()
                .when(matchSummoner.win.isFalse())
                .then(1L)
                .otherwise(0L)
                .sum(); // Long 타입으로 합계

        QMSChampionDTO qmsChampionDTO = new QMSChampionDTO(
            matchSummoner.assists.avg(),
            matchSummoner.deaths.avg(),
            matchSummoner.kills.avg(),
            matchSummoner.championId,
            matchSummoner.championName,
                winCountExpr,
                lossCountExpr,
            challenges.damagePerMinute.avg(),
            challenges.kda.avg(),
            challenges.laneMinionsFirst10Minutes.avg(),
            challenges.damageTakenOnTeamPercentage.avg(),
            challenges.goldPerMinute.avg(),
            matchSummoner.championId.count()
        );

        JPAQuery<MSChampionDTO> query = jpaQueryFactory.select(qmsChampionDTO)
                .from(matchSummoner)
                .join(matchSummoner.challenges, challenges)
                .where(
                        puuidEq(puuid),
                        seasonEq(season)
                )
                .groupBy(matchSummoner.championId, matchSummoner.championName)
                .orderBy(matchSummoner.championId.count().desc());

        return query.fetch();
    }

    @Override
    public List<LinePosition> findAllPositionByPuuidAndLimit(String puuid, Long limit) {

        JPAQuery<LinePosition> query = jpaQueryFactory.select(
                        Projections.fields(
                                LinePosition.class,
                                matchSummoner.individualPosition.as("position"),
                                matchSummoner.individualPosition.count().as("playCount")
                        )
                )
                .from(matchSummoner)
                .where(matchSummoner.puuid.eq(puuid))
                .orderBy(Expressions.stringPath("playCount").desc())
                .groupBy(matchSummoner.individualPosition);

        if(limit != null) {
            query.limit(limit);
        }

        return query.fetch();
    }

    @Override
    public Page<String> findAllMatchIdsByPuuidWithPage(String puuid, Integer queueId, Pageable pageable) {
        List<String> matchIds = jpaQueryFactory.select(matchSummoner.match.matchId).from(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId)
                )
                .orderBy(matchSummoner.match.matchId.desc())
                .offset((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> count = jpaQueryFactory.select(matchSummoner.count())
                .from(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId)
                );


        return PageableExecutionUtils.getPage(matchIds, pageable, count::fetchOne);
    }

    private BooleanExpression queueIdEqOrAll(Integer queueId) {
        return queueId == null ? match.queueId.eq(420).or(match.queueId.eq(440)) : match.queueId.eq(queueId);
    }

    private BooleanExpression puuidEq(String puuid) {
        return StringUtils.hasText(puuid) ? matchSummoner.puuid.eq(puuid) : null;
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        return queueId != null ? match.queueId.eq(queueId) : null;
    }

    private BooleanExpression seasonEq(Integer season) {
        return matchSummoner.match.season.eq(season);
    }
}
