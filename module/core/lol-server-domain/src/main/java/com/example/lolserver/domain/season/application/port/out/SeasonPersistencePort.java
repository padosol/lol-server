package com.example.lolserver.domain.season.application.port.out;

import com.example.lolserver.domain.season.application.model.SeasonReadModel;

import java.util.List;
import java.util.Optional;

public interface SeasonPersistencePort {

    List<SeasonReadModel> findAllSeasons();

    Optional<SeasonReadModel> findById(Long seasonId);
}
