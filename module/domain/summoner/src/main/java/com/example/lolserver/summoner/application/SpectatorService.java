package com.example.lolserver.summoner.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.summoner.application.model.readmodel.CurrentGameInfoReadModel;
import com.example.lolserver.summoner.application.port.in.SpectatorQueryUseCase;
import com.example.lolserver.summoner.application.port.out.SummonerPersistencePort;
import com.example.lolserver.summoner.domain.Summoner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpectatorService implements SpectatorQueryUseCase {

    private final SpectatorFinder spectatorFinder;
    private final SummonerPersistencePort summonerPersistencePort;

    public CurrentGameInfoReadModel getCurrentGameInfo(String puuid, String platformId) {
        return spectatorFinder.getCurrentGameInfo(puuid, platformId);
    }

    @Override
    public CurrentGameInfoReadModel getCurrentGameInfo(String puuid) {
        Summoner summoner = summonerPersistencePort.findById(puuid)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_PUUID, "존재하지 않는 PUUID 입니다. " + puuid));
        summoner.validatePlatformId();
        return spectatorFinder.getCurrentGameInfo(puuid, summoner.getPlatformId());
    }
}
