package com.example.lolserver.web.repository.dsl.impl;

import com.example.lolserver.entity.match.MatchSummoner;
import com.example.lolserver.entity.match.QMatch;
import com.example.lolserver.entity.match.QMatchSummoner;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.repository.dsl.MatchSummonerRepositoryCustom;
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

        Integer queueId = matchRequest.getQueueId();

        List<MatchSummoner> content = jpaQueryFactory.selectFrom(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        puuidEq(matchRequest.getPuuid()),
                        queueIdEq(matchRequest.getQueueId())
                )
                .offset(pageable.getOffset())
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

    private BooleanExpression puuidEq(String puuid) {
        return StringUtils.hasText(puuid) ? QMatchSummoner.matchSummoner.puuid.eq(puuid) : null;
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        return queueId != null ? QMatch.match.queueId.eq(queueId) : null;
    }

}
