package com.example.lolserver.summoner.application.port.in;

import com.example.lolserver.summoner.application.model.readmodel.CurrentGameInfoReadModel;

public interface SpectatorQueryUseCase {

    CurrentGameInfoReadModel getCurrentGameInfo(String puuid, String platformId);

    /**
     * 저장된 소환사의 platformId 로 관전 정보를 조회한다. 저장되지 않은 소환사면 NOT_FOUND_PUUID.
     */
    CurrentGameInfoReadModel getCurrentGameInfo(String puuid);
}
