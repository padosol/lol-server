package com.example.lolserver.web.match.repository.match.dsl;

import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.QMatch;
import com.example.lolserver.web.match.entity.QMatchSummoner;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.lolserver.web.match.entity.QMatch.match;
import static com.example.lolserver.web.match.entity.QMatchSummoner.matchSummoner;

@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<Match> getMatches(MatchRequest matchRequest, Pageable pageable) {

        List<Match> result = jpaQueryFactory.selectFrom(match)
                .join(match.matchSummoners, matchSummoner).on(matchSummoner.puuid.eq(matchRequest.getPuuid()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .where(queueIdEq(matchRequest.getQueueId()))
                .fetch();


        JPAQuery<Match> countQuery = jpaQueryFactory.selectFrom(match)
                .join(match.matchSummoners, matchSummoner).on(matchSummoner.puuid.eq(matchRequest.getPuuid()));

        return PageableExecutionUtils.getPage(result, pageable, () ->  countQuery.fetch().size());
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        if(queueId != null) {
            return match.queueId.eq(queueId);
        }

        return null;
    }

    @Override
    public List<Match> getAllMatches() {
        QMatch match = QMatch.match;
        QMatchSummoner matchSummoner = QMatchSummoner.matchSummoner;

        return jpaQueryFactory.selectFrom(match).fetch();
    }
}
