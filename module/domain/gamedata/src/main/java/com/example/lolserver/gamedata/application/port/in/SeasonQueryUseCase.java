package com.example.lolserver.gamedata.application.port.in;

import com.example.lolserver.gamedata.application.model.SeasonReadModel;

import java.util.List;

public interface SeasonQueryUseCase {

    List<SeasonReadModel> getAllSeasons();

    SeasonReadModel getSeasonById(Long seasonId);
}
