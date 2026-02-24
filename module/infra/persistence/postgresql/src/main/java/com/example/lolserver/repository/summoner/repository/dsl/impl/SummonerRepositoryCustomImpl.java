package com.example.lolserver.repository.summoner.repository.dsl.impl;

import com.example.lolserver.repository.summoner.repository.dsl.SummonerRepositoryCustom;
import com.example.lolserver.repository.summoner.entity.SummonerEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.repository.summoner.entity.QSummonerEntity.summonerEntity;
import static com.example.lolserver.repository.league.entity.QLeagueSummonerEntity.leagueSummonerEntity;

@Repository
@RequiredArgsConstructor
public class SummonerRepositoryCustomImpl implements SummonerRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<SummonerEntity> findAllByGameNameAndTagLineAndPlatformId(
            String gameName, String tagLine, String platformId) {
        return jpaQueryFactory.selectFrom(summonerEntity)
                .where(
                        gameNameEq(gameName),
                        tagLineEq(tagLine),
                        platformIdEq(platformId)
                )
                .fetch();
    }

    @Override
    public List<SummonerEntity> findAllByGameNameAndTagLineAndPlatformIdLike(String q, String platformId) {
        return jpaQueryFactory.selectFrom(summonerEntity)
                .join(leagueSummonerEntity)
                .on(
                        summonerEntity.puuid.eq(leagueSummonerEntity.puuid)
                )
                .where(
                        gameNameLike(q),
                        platformIdEq(platformId)
                ).fetch();
    }

    public BooleanExpression gameNameLike(String gameName) {
        if (!StringUtils.hasText(gameName)) {
            return null;
        }

        // 1. 검색어를 정규화 (공백 제거, 소문자 변환)
        String normalizedKeyword = gameName.replaceAll(" ", "").toLowerCase();

        return summonerEntity.searchName.startsWith(normalizedKeyword);
    }

    public BooleanExpression tagLineLike(String tagLine) {
        return StringUtils.hasText(tagLine) ? summonerEntity.tagLine.contains(tagLine) : null;
    }

    public BooleanExpression gameNameEq(String gameName) {
        return StringUtils.hasText(gameName)
                ? Expressions.stringTemplate(
                        "REPLACE({0}, ' ', '')", summonerEntity.gameName)
                    .equalsIgnoreCase(gameName.replace(" ", ""))
                : null;
    }

    public BooleanExpression tagLineEq(String tagLine) {
        return StringUtils.hasText(tagLine) ? summonerEntity.tagLine.equalsIgnoreCase(tagLine) : null;
    }

    public BooleanExpression platformIdEq(String platformId) {
        return StringUtils.hasText(platformId) ? summonerEntity.platformId.equalsIgnoreCase(platformId) : null;
    }

}
