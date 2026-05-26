package com.example.lolserver.summoner.application.port.in;

import com.example.lolserver.summoner.application.model.SummonerAutoReadModel;
import com.example.lolserver.summoner.application.model.SummonerReadModel;
import com.example.lolserver.summoner.application.model.SummonerRenewalInfoReadModel;
import com.example.lolserver.summoner.domain.SummonerRenewal;
import com.example.lolserver.summoner.domain.vo.GameName;

import java.util.List;
import java.util.Optional;

public interface SummonerQueryUseCase {

    SummonerReadModel getSummoner(GameName gameName, String platformId);

    List<SummonerAutoReadModel> getAllSummonerAutoComplete(String q, String platformId);

    SummonerRenewal renewalSummonerStatus(String puuid);

    SummonerReadModel getSummonerByPuuid(String platformId, String puuid);

    /**
     * DB에 저장된 소환사만 puuid로 조회한다. 없으면 빈 값을 반환하며,
     * {@link #getSummonerByPuuid(String, String)}와 달리 Riot API를 호출하지 않는다.
     * 다른 컨텍스트(duo 등)에서 상대 소환사 표시 정보를 조회할 때 사용한다.
     */
    Optional<SummonerReadModel> findSummonerByPuuid(String puuid);

    List<SummonerRenewalInfoReadModel> getRefreshingSummoners();
}
