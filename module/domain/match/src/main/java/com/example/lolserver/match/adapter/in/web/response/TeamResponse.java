package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.domain.TeamData;

/**
 * 매치 API 응답 - 블루/레드 팀 정보.
 */
public record TeamResponse(
        TeamInfoResponse blueTeam,
        TeamInfoResponse redTeam
) {
    public static TeamResponse from(TeamData data) {
        return new TeamResponse(
                data.getBlueTeam() == null ? null : TeamInfoResponse.from(data.getBlueTeam()),
                data.getRedTeam() == null ? null : TeamInfoResponse.from(data.getRedTeam())
        );
    }
}
