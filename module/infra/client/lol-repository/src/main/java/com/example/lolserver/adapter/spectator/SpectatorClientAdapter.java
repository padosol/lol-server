package com.example.lolserver.adapter.spectator;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorCachePort;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorClientPort;
import com.example.lolserver.mapper.spectator.SpectatorClientMapper;
import com.example.lolserver.restclient.spectator.SpectatorRestClient;
import com.example.lolserver.restclient.spectator.model.CurrentGameInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpectatorClientAdapter implements SpectatorClientPort {

    private final SpectatorRestClient spectatorRestClient;
    private final SpectatorClientMapper spectatorClientMapper;
    private final SpectatorCachePort spectatorCachePort;

    @Override
    public CurrentGameInfoReadModel getCurrentGameInfo(String region, String puuid) {
        try {
            CurrentGameInfoVO vo = spectatorRestClient.getCurrentGameInfoByPuuid(region, puuid);
            if (vo == null) {
                return null;
            }

            CurrentGameInfoReadModel readModel = spectatorClientMapper.toReadModel(vo);

            // 조회 후 모든 참여자 puuid를 키로 캐시에 저장
            spectatorCachePort.saveCurrentGame(region, readModel);

            return readModel;
        } catch (Exception e) {
            log.warn("Failed to get current game info for puuid: {}, region: {}", puuid, region, e);
            return null;
        }
    }
}
