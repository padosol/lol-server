package com.example.lolserver.storage.db.core.repository.summoner.dsl.impl;

import com.example.lolserver.storage.db.core.repository.summoner.dsl.SummonerRepositoryCustom;
import com.example.lolserver.storage.db.core.repository.summoner.dto.QSummonerAutoDTO;
import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerAutoDTO;
import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.storage.db.core.repository.summoner.entity.QSummoner.summoner;
import static com.example.lolserver.storage.db.core.repository.league.entity.QLeague.league;
import static com.example.lolserver.storage.db.core.repository.league.entity.QLeagueSummoner.leagueSummoner;

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
    public List<SummonerAutoDTO> findAllByGameNameAndTagLineAndRegionLike(String q, String region) {
        QSummonerAutoDTO qSummonerAutoDTO = new QSummonerAutoDTO(
                summoner.gameName,
                summoner.tagLine,
                summoner.profileIconId,
                summoner.summonerLevel,
                leagueSummoner.tier,
                leagueSummoner.rank,
                leagueSummoner.leaguePoints
        );

        return jpaQueryFactory.select(qSummonerAutoDTO)
                .from(summoner)
                .join(leagueSummoner)
                .on(
                        summoner.puuid.eq(leagueSummoner.puuid)
                )
                .where(
                        gameNameLike(q),
                        regionEq(region)
                ).fetch();
    }

    public BooleanExpression gameNameLike(String gameName) {
        if (!StringUtils.hasText(gameName)) {
            return null;
        }

        // 1. 검색어를 정규화 (공백 제거, 소문자 변환)
        String normalizedKeyword = gameName.replaceAll(" ", "").toLowerCase();

        return summoner.searchName.startsWith(normalizedKeyword);
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
