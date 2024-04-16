package com.example.lolserver.web.summoner.repository.dsl.impl;

import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.dsl.SummonerRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SummonerRepositoryCustomImpl implements SummonerRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Summoner> findAllByGameNameAndTagLine(Summoner summoner) {
        return null;
    }

    @Override
    public List<Summoner> findAllByGameName(Summoner summoner) {
        return null;
    }
}
