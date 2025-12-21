package com.example.lolserver.repository.summoner;

import com.example.lolserver.domain.summoner.application.port.SummonerPersistencePort;
import com.example.lolserver.domain.summoner.dto.SummonerResponse;
import com.example.lolserver.repository.summoner.entity.SummonerEntity;
import com.example.lolserver.repository.summoner.repository.dsl.SummonerRepositoryCustom;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SummonerPersistenceAdapter implements SummonerPersistencePort {

    private final SummonerRepositoryCustom summonerRepositoryCustom;
    @Override
    public SummonerResponse getSummoner(String gameName, String tagLine, String region) {
        List<SummonerEntity> findSummoner = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegion(
                gameName, tagLine, region);

        if(findSummoner.size() == 1) {
            return SummonerResponse.builder()
                    .profileIconId(findSummoner.get(0).getProfileIconId())
                    .puuid(findSummoner.get(0).getPuuid())
                    .summonerLevel(findSummoner.get(0).getSummonerLevel())
                    .platform(region)
                    .gameName(findSummoner.get(0).getGameName())
                    .tagLine(findSummoner.get(0).getTagLine())
                    .build();
        }

        if (!StringUtils.hasText(tagLine)) {
            throw new CoreException(
                    ErrorType.NOT_FOUND_USER,
                    "존재하지 않는 유저 입니다. " + gameName
            );
        }

        return null;
    }
}
