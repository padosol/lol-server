package com.example.lolserver.esports.application.port.out;

import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.TeamNameHistory;

import java.time.LocalDate;
import java.util.Optional;

public interface TeamPersistencePort {

    boolean exists(String teamId);

    Optional<Team> findById(String teamId);

    Team save(Team team);

    TeamNameHistory saveNameHistory(TeamNameHistory history);

    Optional<TeamNameHistory> findNameHistory(String teamId, Long historyId);

    void deleteNameHistory(Long historyId);

    /** 이 팀 계약 중 마지막 날이 {@code date} 이후(포함)이거나 끝이 없는 것이 있는가 — 해체 검사용. */
    boolean hasContractEffectiveOnOrAfter(String teamId, LocalDate date);
}
