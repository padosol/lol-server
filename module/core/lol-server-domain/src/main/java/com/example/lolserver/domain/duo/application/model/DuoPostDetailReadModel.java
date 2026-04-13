package com.example.lolserver.domain.duo.application.model;

import com.example.lolserver.domain.duo.domain.DuoPost;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DuoPostDetailReadModel {
    private final Long id;
    private final String primaryLane;
    private final String secondaryLane;
    private final boolean hasMicrophone;
    private final String tier;
    private final String rank;
    private final int leaguePoints;
    private final String memo;
    private final String status;
    private final boolean isOwner;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private final List<DuoRequestReadModel> requests;

    public static DuoPostDetailReadModel of(DuoPost post, boolean isOwner,
            List<DuoRequestReadModel> requests) {
        return DuoPostDetailReadModel.builder()
                .id(post.getId())
                .primaryLane(post.getPrimaryLane().name())
                .secondaryLane(post.getSecondaryLane().name())
                .hasMicrophone(post.isHasMicrophone())
                .tier(post.getTier())
                .rank(post.getRank())
                .leaguePoints(post.getLeaguePoints())
                .memo(post.getMemo())
                .status(post.getStatus().name())
                .isOwner(isOwner)
                .expiresAt(post.getExpiresAt())
                .createdAt(post.getCreatedAt())
                .requests(requests)
                .build();
    }
}
