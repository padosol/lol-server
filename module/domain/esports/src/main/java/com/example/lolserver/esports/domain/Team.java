package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 팀 마스터. 1군(LCK)과 CL 팀은 다른 팀이고, CL 팀은 {@code parentTeamId} 로 같은 조직의 LCK 팀을 가리킨다.
 * 재계약·이적·임대의 "같은 팀" 판단은 {@link #organizationId()} 단위다.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Team {

    private String teamId;
    private String teamCode;
    private String gameCode;
    private String homeLeagueId;
    private String parentTeamId;
    private String name;
    private String nameEng;
    private String imageUrl;
    private String darkImageUrl;
    private LocalDate foundedAt;
    private LocalDate disbandedAt;
    private LocalDate nameSince;

    public String organizationId() {
        return parentTeamId != null ? parentTeamId : teamId;
    }

    public TeamName currentName() {
        return new TeamName(teamCode, name, nameEng, imageUrl, darkImageUrl);
    }

    /** 상위 팀은 1단계만 허용한다 — 자기 자신이나, 이미 상위 팀이 있는 팀은 상위 팀이 될 수 없다. */
    public void validateParent(Team parent) {
        if (parent.getTeamId().equals(teamId) || parent.getParentTeamId() != null) {
            throw new CoreException(ErrorType.TEAM_NESTED_TOO_DEEP);
        }
    }

    public void update(String homeLeagueId, String parentTeamId, LocalDate foundedAt) {
        validateLifetime(foundedAt, disbandedAt);
        this.homeLeagueId = homeLeagueId;
        this.parentTeamId = parentTeamId;
        this.foundedAt = foundedAt;
    }

    /**
     * 개명. 현재 이름을 [현재 이름 시작일, 개명일 전날] 이력으로 돌려주고 새 이름을 쓴다.
     * 현재 이름 시작일은 nameSince → foundedAt → previousNameFrom 순으로 정한다.
     */
    public TeamNameHistory rename(TeamName newName, LocalDate renamedOn, LocalDate previousNameFrom, String note) {
        LocalDate validFrom = nameSince != null ? nameSince : foundedAt != null ? foundedAt : previousNameFrom;
        if (validFrom == null || renamedOn == null || !renamedOn.isAfter(validFrom)) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        TeamNameHistory history = TeamNameHistory.builder()
                .teamId(teamId)
                .teamCode(teamCode)
                .name(name)
                .nameEng(nameEng)
                .imageUrl(imageUrl)
                .darkImageUrl(darkImageUrl)
                .validFrom(validFrom)
                .validTo(renamedOn.minusDays(1))
                .note(note)
                .build();
        applyName(newName);
        this.nameSince = renamedOn;
        return history;
    }

    /** 현재 이름 오입력 정정. 이력을 남기지 않는다. */
    public void correctCurrentName(TeamName teamName, LocalDate nameSince) {
        applyName(teamName);
        this.nameSince = nameSince;
    }

    public void disband(LocalDate disbandedAt) {
        if (disbandedAt == null) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        validateLifetime(foundedAt, disbandedAt);
        this.disbandedAt = disbandedAt;
    }

    private void applyName(TeamName teamName) {
        this.teamCode = teamName.teamCode();
        this.name = teamName.name();
        this.nameEng = teamName.nameEng();
        this.imageUrl = teamName.imageUrl();
        this.darkImageUrl = teamName.darkImageUrl();
    }

    private static void validateLifetime(LocalDate foundedAt, LocalDate disbandedAt) {
        if (foundedAt != null && disbandedAt != null && disbandedAt.isBefore(foundedAt)) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
    }
}
