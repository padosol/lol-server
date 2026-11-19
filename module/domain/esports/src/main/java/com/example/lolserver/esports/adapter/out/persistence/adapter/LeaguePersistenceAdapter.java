package com.example.lolserver.esports.adapter.out.persistence.adapter;

import com.example.lolserver.esports.adapter.out.persistence.mapper.EsportsMasterMapper;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsLeagueJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsSeasonJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.support.EsportsConstraintTranslator;
import com.example.lolserver.esports.application.port.out.LeaguePersistencePort;
import com.example.lolserver.esports.domain.League;
import com.example.lolserver.esports.domain.Season;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeaguePersistenceAdapter implements LeaguePersistencePort {

    private final EsportsLeagueJpaRepository leagueRepository;
    private final EsportsSeasonJpaRepository seasonRepository;
    private final EsportsMasterMapper mapper;

    @Override
    public boolean existsLeague(String leagueId) {
        return leagueRepository.existsById(leagueId);
    }

    @Override
    public Optional<League> findLeague(String leagueId) {
        return leagueRepository.findById(leagueId).map(mapper::toDomain);
    }

    @Override
    public League saveLeague(League league) {
        try {
            return mapper.toDomain(leagueRepository.saveAndFlush(mapper.toEntity(league)));
        } catch (DataIntegrityViolationException e) {
            throw EsportsConstraintTranslator.translate(e);
        }
    }

    @Override
    public boolean existsSeason(String seasonId) {
        return seasonRepository.existsById(seasonId);
    }

    @Override
    public Optional<Season> findSeason(String seasonId) {
        return seasonRepository.findById(seasonId).map(mapper::toDomain);
    }

    @Override
    public Season saveSeason(Season season) {
        try {
            return mapper.toDomain(seasonRepository.saveAndFlush(mapper.toEntity(season)));
        } catch (DataIntegrityViolationException e) {
            throw EsportsConstraintTranslator.translate(e);
        }
    }
}
