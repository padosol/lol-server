package com.example.lolserver.esports.domain;

import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

/** 타임라인 테스트용 팀·계약·사건. T1A·GENA 는 각각 T1·GEN 과 같은 조직의 CL 팀이다. */
final class RosterFixtures {

    static final Team T1 = team("t1", null);
    static final Team T1A = team("t1a", "t1");
    static final Team GEN = team("gen", null);
    static final Team GENA = team("gena", "gen");

    static final String PERSON = "faker";

    private RosterFixtures() {
    }

    static LocalDate date(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    static Contract contract(long id, Team team, Role role, Position position, ContractType type,
                             LocalDate start, LocalDate end) {
        return Contract.builder()
                .contractId(id)
                .personId(PERSON)
                .teamId(team.getTeamId())
                .organizationId(team.organizationId())
                .role(role)
                .position(position)
                .contractType(type)
                .startDate(start)
                .endDate(end)
                .build();
    }

    static Contract player(long id, Team team, LocalDate start, LocalDate end) {
        return contract(id, team, Role.PLAYER, Position.MID, ContractType.SIGN, start, end);
    }

    static Contract endedPlayer(long id, Team team, LocalDate start, LocalDate end,
                                LocalDate actualEnd, EndReason reason) {
        return Contract.builder()
                .contractId(id)
                .personId(PERSON)
                .teamId(team.getTeamId())
                .organizationId(team.organizationId())
                .role(Role.PLAYER)
                .position(Position.MID)
                .contractType(ContractType.SIGN)
                .startDate(start)
                .endDate(end)
                .actualEndDate(actualEnd)
                .endReason(reason)
                .build();
    }

    static CareerEvent event(long id, CareerEventType type, LocalDate date) {
        return CareerEvent.builder()
                .eventId(id)
                .personId(PERSON)
                .eventType(type)
                .eventDate(date)
                .build();
    }

    private static Team team(String teamId, String parentTeamId) {
        return Team.builder().teamId(teamId).teamCode(teamId.toUpperCase()).gameCode("lol").name(teamId)
                .parentTeamId(parentTeamId).build();
    }
}
