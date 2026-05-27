package com.example.lolserver.gamedata.application.port.out;

import com.example.lolserver.gamedata.application.model.readmodel.SeasonReadModel;

import java.util.List;
import java.util.Optional;

public interface SeasonPersistencePort {

    List<SeasonReadModel> findAllSeasons();

    Optional<SeasonReadModel> findById(Long seasonId);
}
