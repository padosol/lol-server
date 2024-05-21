package com.example.lolserver.web.match.repository.matchsummoner.dsl.impl;

import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.QMatch;
import com.example.lolserver.web.match.entity.QMatchSummoner;
import com.example.lolserver.web.match.repository.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MatchSummonerRepositoryCustomImpl implements MatchSummonerRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public Page<MatchSummoner> findAllByPuuidAndQueueId(MatchRequest matchRequest, Pageable pageable) {

        QMatchSummoner matchSummoner = QMatchSummoner.matchSummoner;
        QMatch match = QMatch.match;

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

        QMatch match = QMatch.match;
        List<String> inData = jpaQueryFactory.select(match.matchId)
                .from(match)
                .where(match.matchId.in(matchIds))
                .fetch();

        return matchIds.stream().filter( matchId -> !inData.contains(matchId)).toList();
    }

    private BooleanExpression puuidEq(String puuid) {
        return StringUtils.hasText(puuid) ? QMatchSummoner.matchSummoner.puuid.eq(puuid) : null;
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        return queueId != null ? QMatch.match.queueId.eq(queueId) : null;
    }

}
