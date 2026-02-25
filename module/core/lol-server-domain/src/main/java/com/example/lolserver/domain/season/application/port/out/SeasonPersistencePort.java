package com.example.lolserver.domain.season.application.port.out;

import com.example.lolserver.domain.season.application.dto.SeasonResponse;

import java.util.List;
import java.util.Optional;

public interface SeasonPersistencePort {

    List<SeasonResponse> findAllSeasons();

    Optional<SeasonResponse> findById(Long seasonId);
}
