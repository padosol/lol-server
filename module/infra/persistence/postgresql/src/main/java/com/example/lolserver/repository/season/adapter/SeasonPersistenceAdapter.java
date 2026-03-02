package com.example.lolserver.repository.season.adapter;

import com.example.lolserver.domain.season.application.model.SeasonReadModel;
import com.example.lolserver.domain.season.application.port.out.SeasonPersistencePort;
import com.example.lolserver.repository.season.SeasonJpaRepository;
import com.example.lolserver.repository.season.mapper.SeasonMapper;
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
