package com.example.lolserver.esports.adapter.out.persistence.adapter;

import com.example.lolserver.esports.adapter.out.persistence.mapper.EsportsMasterMapper;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsContractJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsTeamJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsTeamNameHistoryJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.support.EsportsConstraintTranslator;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.TeamNameHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TeamPersistenceAdapter implements TeamPersistencePort {

    private final EsportsTeamJpaRepository teamRepository;
    private final EsportsTeamNameHistoryJpaRepository nameHistoryRepository;
    private final EsportsContractJpaRepository contractRepository;
    private final EsportsMasterMapper mapper;

    @Override
    public boolean exists(String teamId) {
        return teamRepository.existsById(teamId);
    }

    @Override
    public Optional<Team> findById(String teamId) {
        return teamRepository.findById(teamId).map(mapper::toDomain);
    }

    @Override
    public Team save(Team team) {
        try {
            return mapper.toDomain(teamRepository.saveAndFlush(mapper.toEntity(team)));
        } catch (DataIntegrityViolationException e) {
            throw EsportsConstraintTranslator.translate(e);
        }
    }

    @Override
    public boolean hasChildTeams(String teamId) {
        return teamRepository.existsByParentTeamId(teamId);
    }

    @Override
    public TeamNameHistory saveNameHistory(TeamNameHistory history) {
        try {
            return mapper.toDomain(nameHistoryRepository.saveAndFlush(mapper.toEntity(history)));
        } catch (DataIntegrityViolationException e) {
            throw EsportsConstraintTranslator.translate(e);
        }
    }

    @Override
    public Optional<TeamNameHistory> findNameHistory(String teamId, Long historyId) {
        return nameHistoryRepository.findByHistoryIdAndTeamId(historyId, teamId).map(mapper::toDomain);
    }

    @Override
    public void deleteNameHistory(Long historyId) {
        nameHistoryRepository.deleteById(historyId);
        nameHistoryRepository.flush();
    }

    @Override
    public boolean hasContractEffectiveOnOrAfter(String teamId, LocalDate date) {
        return contractRepository.existsEffectiveOnOrAfter(teamId, date);
    }
}
