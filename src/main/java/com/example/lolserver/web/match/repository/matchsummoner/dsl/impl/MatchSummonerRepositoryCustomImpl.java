package com.example.lolserver.web.match.repository.matchsummoner.dsl.impl;

import com.example.lolserver.web.match.dto.MSChampionResponse;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.QMatch;
import com.example.lolserver.web.match.entity.QMatchSummoner;
import com.example.lolserver.web.match.repository.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Ops;
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
    public List<MSChampionResponse> findAllChampionKDAByPuuidAndSeasonAndQueueType(String puuid, int season, int queueType) {

        List<Tuple> fetch = jpaQueryFactory.select(
                        matchSummoner.championId,
                        matchSummoner.championName,
                        matchSummoner.kills.avg().as("kills"),
                        matchSummoner.deaths.avg().as("deaths"),
                        matchSummoner.assists.avg().as("assists"),
                        matchSummoner.neutralMinionsKilled.add(matchSummoner.totalMinionsKilled).avg().as("cs"),
                        match.gameDuration.avg().as("duration"),
                        matchSummoner.count().as("playCount")
                ).from(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        matchSummoner.puuid.eq(puuid),
                        match.season.eq(season),
                        match.queueId.eq(queueType)
                )
                .groupBy(matchSummoner.championId, matchSummoner.championName)
                .orderBy(Expressions.stringPath("playCount").desc())
                .fetch();


//        select
//        ms.champion_id ,
//                ms.champion_name,
//                round(avg(ms.kills), 1) kills,
//                round(avg(ms.deaths), 1) deaths,
//                round(avg(ms.assists), 1) assists,
//                round(avg(ms.neutral_minions_killed + ms.total_minions_killed)) cs,
//                round(avg(m.game_duration)) duration,
//                count(ms.puuid) playCount
//        from match_summoner ms
//        join "match" m on m.match_id  = ms.match_id
//        where ms.puuid = 'TF8jxtZ_8d98Rlwb9C2CRewl1yBN4P_1GzlOWJ36Nbehjj8ZIRdKWa3qEVPCGUoJONsrR1W-ql6KcA'
//        and m.season = 23
//        group by ms.champion_id, ms.champion_name
//        order by playCount desc;


        return null;
    }

    private BooleanExpression puuidEq(String puuid) {
        return StringUtils.hasText(puuid) ? matchSummoner.puuid.eq(puuid) : null;
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        return queueId != null ? match.queueId.eq(queueId) : null;
    }

}
