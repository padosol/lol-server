package com.example.lolserver.domain.duo.domain;

import com.example.lolserver.domain.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.domain.duo.domain.vo.Lane;
import com.example.lolserver.domain.duo.domain.vo.TierInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DuoPost {

    private Long id;
    private Long memberId;
    private String puuid;
    private Lane primaryLane;
    private Lane secondaryLane;
    private boolean hasMicrophone;
    private String tier;
    private String rank;
    private int leaguePoints;
    private String memo;
    private DuoPostStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DuoPost create(Long memberId, String puuid,
            Lane primaryLane, Lane secondaryLane,
            boolean hasMicrophone, TierInfo tierInfo, String memo) {
        LocalDateTime now = LocalDateTime.now();
        return DuoPost.builder()
                .memberId(memberId)
                .puuid(puuid)
                .primaryLane(primaryLane)
                .secondaryLane(secondaryLane)
                .hasMicrophone(hasMicrophone)
                .tier(tierInfo.tier())
                .rank(tierInfo.rank())
                .leaguePoints(tierInfo.leaguePoints())
                .memo(memo)
                .status(DuoPostStatus.ACTIVE)
                .expiresAt(now.plusHours(24))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean isOwner(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isActive() {
        return this.status == DuoPostStatus.ACTIVE
                && LocalDateTime.now().isBefore(this.expiresAt);
    }

    public void markMatched() {
        this.status = DuoPostStatus.MATCHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markDeleted() {
        this.status = DuoPostStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status = DuoPostStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateContent(Lane primaryLane, Lane secondaryLane,
                              boolean hasMicrophone, String memo) {
        this.primaryLane = primaryLane;
        this.secondaryLane = secondaryLane;
        this.hasMicrophone = hasMicrophone;
        this.memo = memo;
        this.updatedAt = LocalDateTime.now();
    }
}
