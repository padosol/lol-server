package com.example.lolserver.web.summoner.repository.dsl.impl;

import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.QMatch;
import com.example.lolserver.web.match.entity.QMatchSummoner;
import com.example.lolserver.web.summoner.entity.QSummoner;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.dsl.SummonerRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SummonerRepositoryCustomImpl implements SummonerRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public Summoner findByGameNameAndTagLine(String gameName, String tagLine) {
        return null;
    }

    @Override
    public List<Summoner> findAllByGameNameAndTagLineAndRegion(String gameName, String tagLine, String region) {

        QSummoner summoner = QSummoner.summoner;

        List<Summoner> result = jpaQueryFactory.selectFrom(summoner)
                .where(
                        gameNameEq(gameName),
                        tagLineEq(tagLine),
                        regionEq(region)
                )
                .fetch();

        return result;
    }

    public BooleanExpression gameNameEq(String gameName) {
        return StringUtils.hasText(gameName) ? QSummoner.summoner.gameName.eq(gameName) : null;
    }

    public BooleanExpression tagLineEq(String tagLine) {
        return StringUtils.hasText(tagLine) ? QSummoner.summoner.tagLine.eq(tagLine) : null;
    }

    public BooleanExpression regionEq(String region) {
        return StringUtils.hasText(region) ? QSummoner.summoner.region.eq(region) : null;
    }

}
