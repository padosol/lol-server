package com.example.lolserver.domain.summoner.application.port.in;

import com.example.lolserver.domain.summoner.application.model.SummonerAutoReadModel;
import com.example.lolserver.domain.summoner.application.model.SummonerReadModel;
import com.example.lolserver.domain.summoner.application.model.SummonerRenewalInfoReadModel;
import com.example.lolserver.domain.summoner.domain.SummonerRenewal;
import com.example.lolserver.domain.summoner.domain.vo.GameName;

import java.util.List;

public interface SummonerQueryUseCase {

    SummonerReadModel getSummoner(GameName gameName, String platformId);

    List<SummonerAutoReadModel> getAllSummonerAutoComplete(String q, String platformId);

    SummonerRenewal renewalSummonerStatus(String puuid);

    SummonerReadModel getSummonerByPuuid(String platformId, String puuid);

    List<SummonerRenewalInfoReadModel> getRefreshingSummoners();
}
