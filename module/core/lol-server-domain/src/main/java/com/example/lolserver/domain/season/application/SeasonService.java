package com.example.lolserver.domain.season.application;

import com.example.lolserver.domain.season.application.dto.SeasonResponse;
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

    public List<SeasonResponse> getAllSeasons() {
        return seasonPersistencePort.findAllSeasons();
    }

    public SeasonResponse getSeasonById(Long seasonId) {
        return seasonPersistencePort.findById(seasonId).orElse(null);
    }
}
