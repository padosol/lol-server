package com.example.lolserver.repository.summoner;

import com.example.lolserver.domain.summoner.application.port.out.SummonerPersistencePort;
import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.repository.summoner.entity.SummonerEntity;
import com.example.lolserver.repository.summoner.repository.SummonerJpaRepository;
import com.example.lolserver.repository.summoner.repository.dsl.SummonerRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SummonerPersistenceAdapter implements SummonerPersistencePort {

    private final SummonerRepositoryCustom summonerRepositoryCustom;
    private final SummonerJpaRepository summonerJpaRepository;
    private final SummonerMapper summonerMapper;

    @Override
    public Optional<Summoner> getSummoner(String gameName, String tagLine, String platformId) {
        List<SummonerEntity> findSummoner = summonerRepositoryCustom.findAllByGameNameAndTagLineAndPlatformId(
                gameName, tagLine, platformId);

        if (findSummoner.size() == 1) {
            return Optional.of(summonerMapper.toDomain(findSummoner.get(0)));
        }

        return Optional.empty();
    }

    @Override
    public List<Summoner> getSummonerAuthComplete(String q, String platformId) {
        List<SummonerEntity> summonerEntities = summonerRepositoryCustom.findAllByGameNameAndTagLineAndPlatformIdLike(
                q, platformId
        );

        return summonerMapper.toDomainList(summonerEntities);
    }

    @Override
    public Optional<Summoner> findById(String puuid) {
        return summonerJpaRepository.findById(puuid)
                .map(summonerMapper::toDomain);
    }

    @Override
    public Summoner save(Summoner summoner) {
        SummonerEntity summonerEntity = summonerMapper.toEntity(summoner);
        SummonerEntity savedSummoner = summonerJpaRepository.save(summonerEntity);
        return summonerMapper.toDomain(savedSummoner);
    }

    @Override
    public List<Summoner> findAllByPuuidIn(Collection<String> puuids) {
        if (puuids == null || puuids.isEmpty()) {
            return Collections.emptyList();
        }
        List<SummonerEntity> entities = summonerJpaRepository.findAllByPuuidIn(puuids);
        return summonerMapper.toDomainList(entities);
    }
}
