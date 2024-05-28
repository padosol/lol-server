package com.example.lolserver.web.match.repository.matchsummoner.dsl.impl;

import com.example.lolserver.web.match.dto.MSChampionResponse;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.QMatch;
import com.example.lolserver.web.match.entity.QMatchSummoner;
import com.example.lolserver.web.match.repository.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.web.match.entity.QMatch.match;
import static com.example.lolserver.web.match.entity.QMatchSummoner.matchSummoner;

@Repository
@RequiredArgsConstructor
public class MatchSummonerRepositoryCustomImpl implements MatchSummonerRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public Page<MatchSummoner> findAllByPuuidAndQueueId(MatchRequest matchRequest, Pageable pageable) {

        List<MatchSummoner> content = jpaQueryFactory.selectFrom(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        puuidEq(matchRequest.getPuuid()),
                        queueIdEq(matchRequest.getQueueId())
                )
                .orderBy(matchSummoner.match.matchId.desc())
                .offset((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> count = jpaQueryFactory.select(matchSummoner.count())
                .from(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        puuidEq(matchRequest.getPuuid()),
                        queueIdEq(matchRequest.getQueueId())
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
    public List<MSChampionResponse> findAllChampionKDAByPuuidAndSeasonAndQueueType(String puuid, Integer season, Integer queueType) {

        List<MSChampionResponse> result = jpaQueryFactory.select(
                        Projections.fields(MSChampionResponse.class,
                                matchSummoner.championId,
                                matchSummoner.championName,
                                Expressions.template(Double.class, "ROUND({0}, 2)", matchSummoner.kills.avg()).as("kills"),
                                Expressions.template(Double.class, "ROUND({0}, 2)", matchSummoner.deaths.avg()).as("deaths"),
                                Expressions.template(Double.class, "ROUND({0}, 2)", matchSummoner.assists.avg()).as("assists"),
                                Expressions.template(Double.class, "ROUND({0}, 2)", matchSummoner.assists.avg()).as("assists"),
                                Expressions.template(Double.class, "ROUND({0}, 2)", matchSummoner.neutralMinionsKilled.add(matchSummoner.totalMinionsKilled).avg()).as("cs"),
                                Expressions.template(Double.class, "ROUND({0}, 2)", match.gameDuration.avg()).as("duration"),
                                matchSummoner.count().as("playCount")
                        )
                ).from(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        matchSummoner.puuid.eq(puuid),
                        match.season.eq(season),
                        queueIdEqOrAll(queueType)
                )
                .groupBy(matchSummoner.championId, matchSummoner.championName)
                .limit(7)
                .orderBy(Expressions.stringPath("playCount").desc())
                .fetch();

        return result;
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

}
