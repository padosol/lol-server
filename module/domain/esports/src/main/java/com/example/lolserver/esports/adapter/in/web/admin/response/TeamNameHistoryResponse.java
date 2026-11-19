package com.example.lolserver.esports.adapter.in.web.admin.response;

import com.example.lolserver.esports.domain.TeamNameHistory;

import java.time.LocalDate;

public record TeamNameHistoryResponse(
        Long historyId,
        String teamId,
        String teamCode,
        String name,
        String nameEng,
        String imageUrl,
        String darkImageUrl,
        LocalDate validFrom,
        LocalDate validTo,
        String note
) {
    public static TeamNameHistoryResponse from(TeamNameHistory history) {
        return new TeamNameHistoryResponse(history.getHistoryId(), history.getTeamId(), history.getTeamCode(),
                history.getName(), history.getNameEng(), history.getImageUrl(), history.getDarkImageUrl(),
                history.getValidFrom(), history.getValidTo(), history.getNote());
    }
}
