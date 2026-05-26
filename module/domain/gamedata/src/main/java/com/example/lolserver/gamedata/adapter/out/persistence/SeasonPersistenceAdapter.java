package com.example.lolserver.gamedata.adapter.out.persistence;

import com.example.lolserver.gamedata.application.model.SeasonReadModel;
import com.example.lolserver.gamedata.application.port.out.SeasonPersistencePort;
import com.example.lolserver.gamedata.adapter.out.persistence.mapper.SeasonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SeasonPersistenceAdapter implements SeasonPersistencePort {

    private final SeasonJpaRepository seasonJpaRepository;
    private final SeasonMapper seasonMapper;

    @Override
    public List<SeasonReadModel> findAllSeasons() {
        return seasonJpaRepository.findAllWithPatchVersions()
                .stream()
                .map(seasonMapper::toReadModel)
                .toList();
    }

    @Override
    public Optional<SeasonReadModel> findById(Long seasonId) {
        return seasonJpaRepository.findByIdWithPatchVersions(seasonId)
                .map(seasonMapper::toReadModel);
    }
}
