package com.example.lolserver.domain.duo.application.model;

import com.example.lolserver.domain.duo.domain.DuoPost;
import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DuoPostReadModel {
    private final Long id;
    private final String primaryLane;
    private final String desiredLane;
    private final boolean hasMicrophone;
    private final String tier;
    private final String rank;
    private final int leaguePoints;
    private final String memo;
    private final String status;
    private final boolean tierAvailable;
    private final List<MostChampion> mostChampions;
    private final RecentGameSummary recentGameSummary;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;

    public static DuoPostReadModel of(DuoPost post) {
        return DuoPostReadModel.builder()
                .id(post.getId())
                .primaryLane(post.getPrimaryLane().name())
                .desiredLane(post.getDesiredLane().name())
                .hasMicrophone(post.isHasMicrophone())
                .tier(post.getTier())
                .rank(post.getRank())
                .leaguePoints(post.getLeaguePoints())
                .memo(post.getMemo())
                .status(post.getStatus().name())
                .tierAvailable(post.getTier() != null)
                .mostChampions(post.getMostChampions())
                .recentGameSummary(post.getRecentGameSummary())
                .expiresAt(post.getExpiresAt())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
