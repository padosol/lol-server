package com.example.lolserver.repository.match.match.dsl;

import com.example.lolserver.repository.match.entity.MatchEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.repository.match.entity.QMatchSummonerEntity.matchSummonerEntity;
import static com.example.lolserver.repository.match.entity.QMatchEntity.matchEntity;
import static com.example.lolserver.repository.match.entity.QChallengesEntity.challengesEntity;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<MatchEntity> getMatches(String puuid, Integer queueId, Pageable pageable) {

        List<String> matchIds = jpaQueryFactory
                .select(matchSummonerEntity.matchEntity.matchId)
                .from(matchSummonerEntity)
                .join(matchSummonerEntity.matchEntity, matchEntity)
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId),
                        matchEntity.gameMode.equalsIgnoreCase("CLASSIC").or(matchEntity.gameMode.equalsIgnoreCase("CHERRY"))
                )
                .orderBy(matchSummonerEntity.matchEntity.matchId.desc())
                .offset((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .fetch();

        List<MatchEntity> result = jpaQueryFactory.selectFrom(matchEntity)
                .join(matchEntity.matchSummonerEntities, matchSummonerEntity).fetchJoin()
//                .join(matchSummonerEntity.challengesEntity, challengesEntity).fetchJoin()
                .where(
                        matchEntity.matchId.in(matchIds),
                        queueIdEq(queueId)
//                        match.gameMode.equalsIgnoreCase("CLASSIC").or(match.gameMode.equalsIgnoreCase("CHERRY"))
                )
                .orderBy(matchEntity.gameEndTimestamp.desc())
                .fetch();

        JPAQuery<MatchEntity> countQuery = jpaQueryFactory.selectFrom(matchEntity)
                .join(matchEntity.matchSummonerEntities, matchSummonerEntity)
                .where(matchSummonerEntity.matchSummonerId.puuid.eq(puuid));

        return PageableExecutionUtils.getPage(result, pageable, () ->  countQuery.fetch().size());
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        if(queueId != null) {
            return matchEntity.queueId.eq(queueId);
        }

        return null;
    }

    private BooleanExpression puuidEq(String puuid) {
        return StringUtils.hasText(puuid) ? matchSummonerEntity.matchSummonerId.puuid.eq(puuid) : null;
    }
}
