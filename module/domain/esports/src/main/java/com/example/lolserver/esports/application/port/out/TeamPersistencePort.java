package com.example.lolserver.esports.application.port.out;

import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.TeamNameHistory;

import java.time.LocalDate;
import java.util.Optional;

public interface TeamPersistencePort {

    boolean exists(String teamId);

    Optional<Team> findById(String teamId);

    Team save(Team team);

    /** 이 팀을 상위 팀으로 가리키는 팀(CL 팀)이 있는가 — 상위 팀 1단계 검사용. */
    boolean hasChildTeams(String teamId);

    TeamNameHistory saveNameHistory(TeamNameHistory history);

    Optional<TeamNameHistory> findNameHistory(String teamId, Long historyId);

    void deleteNameHistory(Long historyId);

    /** 이 팀 계약 중 마지막 날이 {@code date} 이후(포함)이거나 끝이 없는 것이 있는가 — 해체 검사용. */
    boolean hasContractEffectiveOnOrAfter(String teamId, LocalDate date);
}
