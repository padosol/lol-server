package com.example.lolserver.storage.db.core.repository.match.match.dsl;

import com.example.lolserver.storage.db.core.repository.match.entity.Match;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.storage.db.core.repository.match.entity.QMatch.match;
import static com.example.lolserver.storage.db.core.repository.match.entity.QMatchSummoner.matchSummoner;
import static com.example.lolserver.storage.db.core.repository.match.entity.QChallenges.challenges;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Page<Match> getMatches(String puuid, Integer queueId, Pageable pageable) {

        List<String> matchIds = jpaQueryFactory.select(matchSummoner.match.matchId).from(matchSummoner)
                .join(matchSummoner.match, match)
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId),
                        match.gameMode.equalsIgnoreCase("CLASSIC").or(match.gameMode.equalsIgnoreCase("CHERRY"))
                )
                .orderBy(matchSummoner.match.matchId.desc())
                .offset((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .fetch();

        List<Match> result = jpaQueryFactory.selectFrom(match)
                .join(match.matchSummoners, matchSummoner).fetchJoin()
                .join(matchSummoner.challenges, challenges).fetchJoin()
                .where(
                        match.matchId.in(matchIds),
                        queueIdEq(queueId)
//                        match.gameMode.equalsIgnoreCase("CLASSIC").or(match.gameMode.equalsIgnoreCase("CHERRY"))
                )
                .orderBy(match.gameEndTimestamp.desc())
                .fetch();

        JPAQuery<Match> countQuery = jpaQueryFactory.selectFrom(match)
                .join(match.matchSummoners, matchSummoner)
                .where(matchSummoner.puuid.eq(puuid));

        return PageableExecutionUtils.getPage(result, pageable, () ->  countQuery.fetch().size());
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        if(queueId != null) {
            return match.queueId.eq(queueId);
        }

        return null;
    }

    private BooleanExpression puuidEq(String puuid) {
        return StringUtils.hasText(puuid) ? matchSummoner.puuid.eq(puuid) : null;
    }
}
