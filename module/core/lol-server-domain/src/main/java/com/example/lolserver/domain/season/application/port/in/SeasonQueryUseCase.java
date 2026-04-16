package com.example.lolserver.domain.season.application.port.in;

import com.example.lolserver.domain.season.application.model.SeasonReadModel;

import java.util.List;

public interface SeasonQueryUseCase {

    List<SeasonReadModel> getAllSeasons();

    SeasonReadModel getSeasonById(Long seasonId);
}
