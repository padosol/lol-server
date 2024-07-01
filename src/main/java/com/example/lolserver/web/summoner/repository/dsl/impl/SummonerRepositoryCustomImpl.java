package com.example.lolserver.web.summoner.repository.dsl.impl;

import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.dsl.SummonerRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.web.league.entity.QLeagueSummoner.leagueSummoner;
import static com.example.lolserver.web.summoner.entity.QSummoner.summoner;
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
        return StringUtils.hasText(gameName) ? Expressions.stringTemplate("REPLACE({0}, ' ', '')", summoner.gameName).equalsIgnoreCase(gameName.replace(" ","")) : null;
    }

    public BooleanExpression tagLineEq(String tagLine) {
        return StringUtils.hasText(tagLine) ? summoner.tagLine.equalsIgnoreCase(tagLine) : null;
    }

    public BooleanExpression regionEq(String region) {
        return StringUtils.hasText(region) ? summoner.region.equalsIgnoreCase(region) : null;
    }

}
