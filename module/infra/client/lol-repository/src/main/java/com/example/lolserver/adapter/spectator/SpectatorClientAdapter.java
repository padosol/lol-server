package com.example.lolserver.adapter.spectator;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorCachePort;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorClientPort;
import com.example.lolserver.mapper.spectator.SpectatorClientMapper;
import com.example.lolserver.restclient.spectator.SpectatorRestClient;
import com.example.lolserver.restclient.spectator.model.CurrentGameInfoVO;
import com.example.lolserver.domain.spectator.application.model.ParticipantReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpectatorClientAdapter implements SpectatorClientPort {

    private final SpectatorRestClient spectatorRestClient;
    private final SpectatorClientMapper spectatorClientMapper;
    private final SpectatorCachePort spectatorCachePort;

    @Override
    public CurrentGameInfoReadModel getCurrentGameInfo(String platformId, String puuid) {
        try {
            CurrentGameInfoVO vo = spectatorRestClient.getCurrentGameInfoByPuuid(platformId, puuid);
            if (vo == null) {
                // Negative Caching: 게임 중이 아님을 캐싱
                spectatorCachePort.saveNoGame(platformId, puuid);
                return null;
            }

            CurrentGameInfoReadModel readModel = spectatorClientMapper.toReadModel(vo);

            // 조회 후 모든 참여자 puuid를 키로 캐시에 저장
            spectatorCachePort.saveCurrentGame(platformId, readModel);

            // 게임 메타데이터 캐싱 (삭제용)
            List<String> puuids = readModel.participants().stream()
                    .map(ParticipantReadModel::puuid)
                    .toList();
            spectatorCachePort.saveGameMeta(platformId, readModel.gameId(), readModel.gameStartTime(), puuids);

            return readModel;
        } catch (Exception e) {
            log.warn("Failed to get current game info for puuid: {}, platformId: {}", puuid, platformId, e);
            return null;
        }
    }
}
