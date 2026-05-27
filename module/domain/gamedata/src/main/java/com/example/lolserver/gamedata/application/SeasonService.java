package com.example.lolserver.gamedata.application;

import com.example.lolserver.gamedata.application.model.readmodel.SeasonReadModel;
import com.example.lolserver.gamedata.application.port.in.SeasonQueryUseCase;
import com.example.lolserver.gamedata.application.port.out.SeasonPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonService implements SeasonQueryUseCase {

    private final SeasonPersistencePort seasonPersistencePort;

    public List<SeasonReadModel> getAllSeasons() {
        return seasonPersistencePort.findAllSeasons();
    }

    public SeasonReadModel getSeasonById(Long seasonId) {
        return seasonPersistencePort.findById(seasonId).orElse(null);
    }
}
