package com.example.lolserver.repository.season.adapter;

import com.example.lolserver.domain.season.application.dto.SeasonResponse;
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
    public List<SeasonResponse> findAllSeasons() {
        return seasonJpaRepository.findAllWithPatchVersions()
                .stream()
                .map(seasonMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<SeasonResponse> findById(Long seasonId) {
        return seasonJpaRepository.findByIdWithPatchVersions(seasonId)
                .map(seasonMapper::toResponse);
    }
}
