package com.example.lolserver.domain.season.application;

import com.example.lolserver.domain.season.application.model.SeasonReadModel;
import com.example.lolserver.domain.season.application.port.out.SeasonPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonService {

    private final SeasonPersistencePort seasonPersistencePort;

    public List<SeasonReadModel> getAllSeasons() {
        return seasonPersistencePort.findAllSeasons();
    }

    public SeasonReadModel getSeasonById(Long seasonId) {
        return seasonPersistencePort.findById(seasonId).orElse(null);
    }
}
