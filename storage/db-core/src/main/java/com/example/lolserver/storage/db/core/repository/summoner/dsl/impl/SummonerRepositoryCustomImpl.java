package com.example.lolserver.storage.db.core.repository.summoner.dsl.impl;

import com.example.lolserver.storage.db.core.repository.summoner.dsl.SummonerRepositoryCustom;
import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.storage.db.core.repository.summoner.entity.QSummoner.summoner;

@Repository
@RequiredArgsConstructor
public class SummonerRepositoryCustomImpl implements SummonerRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Summoner> findAllByGameNameAndTagLineAndRegion(String gameName, String tagLine, String region) {
        return jpaQueryFactory.selectFrom(summoner)
                .where(
                        gameNameEq(gameName),
                        tagLineEq(tagLine),
                        regionEq(region)
                )
                .fetch();
    }

    @Override
    public List<Summoner> findAllByGameNameAndTagLineAndRegionLike(String gameName, String tagLine, String region) {

        return jpaQueryFactory.selectFrom(summoner)
                .where(
                        gameNameLike(gameName),
                        tagLineLike(tagLine),
                        regionEq(region)
                )
                .fetch();
    }

    public BooleanExpression gameNameLike(String gameName) {
        return StringUtils.hasText(gameName) ? Expressions.stringTemplate("REPLACE({0}, ' ', '')", summoner.gameName.toLowerCase()).contains(gameName.replaceAll(" ", "").toLowerCase()) : null;
    }

    public BooleanExpression tagLineLike(String tagLine) {
        return StringUtils.hasText(tagLine) ? summoner.tagLine.contains(tagLine) : null;
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
