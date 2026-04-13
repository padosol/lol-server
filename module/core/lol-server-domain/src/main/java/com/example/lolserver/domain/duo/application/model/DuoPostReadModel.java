package com.example.lolserver.domain.duo.application.model;

import com.example.lolserver.domain.duo.domain.DuoPost;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DuoPostReadModel {
    private final Long id;
    private final String primaryLane;
    private final String secondaryLane;
    private final boolean hasMicrophone;
    private final String tier;
    private final String rank;
    private final int leaguePoints;
    private final String memo;
    private final String status;
    private final boolean tierAvailable;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;

    public static DuoPostReadModel of(DuoPost post) {
        return DuoPostReadModel.builder()
                .id(post.getId())
                .primaryLane(post.getPrimaryLane().name())
                .secondaryLane(post.getSecondaryLane().name())
                .hasMicrophone(post.isHasMicrophone())
                .tier(post.getTier())
                .rank(post.getRank())
                .leaguePoints(post.getLeaguePoints())
                .memo(post.getMemo())
                .status(post.getStatus().name())
                .tierAvailable(post.getTier() != null)
                .expiresAt(post.getExpiresAt())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
