package com.example.lolserver.summoner.application.port.in;

import com.example.lolserver.summoner.application.model.readmodel.LeagueReadModel;
import com.example.lolserver.summoner.domain.League;

import java.util.List;

public interface LeagueQueryUseCase {

    List<League> getLeaguesBypuuid(String puuid);

    /**
     * 다른 컨텍스트에 노출하기 위한 리그 요약 조회.
     * puuid의 모든 큐 리그를 ReadModel로 반환한다(히스토리 미포함).
     */
    List<LeagueReadModel> getLeagueSummariesByPuuid(String puuid);
}
