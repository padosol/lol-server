package com.example.lolserver.duo.domain;

import com.example.lolserver.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.duo.domain.vo.Lane;
import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.TierInfo;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DuoPost {

    private Long id;
    private Long memberId;
    private String puuid;
    private Lane primaryLane;
    private Lane desiredLane;
    private boolean hasMicrophone;
    private String tier;
    private String rank;
    private int leaguePoints;
    private String memo;
    private DuoPostStatus status;
    private List<MostChampion> mostChampions;
    private RecentGameSummary recentGameSummary;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DuoPost create(Long memberId, String puuid,
            Lane primaryLane, Lane desiredLane, boolean hasMicrophone, String memo,
            RiotAccountStats stats) {
        TierInfo tierInfo = stats.tierInfo();
        tierInfo.validateRanked();

        LocalDateTime now = LocalDateTime.now();
        return DuoPost.builder()
                .memberId(memberId)
                .puuid(puuid)
                .primaryLane(primaryLane)
                .desiredLane(desiredLane)
                .hasMicrophone(hasMicrophone)
                .tier(tierInfo.tier())
                .rank(tierInfo.rank())
                .leaguePoints(tierInfo.leaguePoints())
                .memo(memo)
                .status(DuoPostStatus.ACTIVE)
                .mostChampions(stats.mostChampions())
                .recentGameSummary(stats.recentGameSummary())
                .expiresAt(now.plusHours(1))
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

    public void validateOwner(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }
    }

    public void validateNotOwner(Long memberId) {
        if (this.memberId.equals(memberId)) {
            throw new CoreException(ErrorType.DUO_POST_SELF_REQUEST);
        }
    }

    public void validateActive() {
        if (!isActive()) {
            throw new CoreException(ErrorType.DUO_POST_NOT_ACTIVE);
        }
    }

    public void markMatched() {
        validateActive();
        this.status = DuoPostStatus.MATCHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void validateMatched() {
        if (this.status != DuoPostStatus.MATCHED) {
            throw new CoreException(ErrorType.DUO_POST_NOT_MATCHED);
        }
    }

    public void markDeleted() {
        if (this.status == DuoPostStatus.MATCHED) {
            throw new CoreException(ErrorType.DUO_POST_ALREADY_MATCHED);
        }
        this.status = DuoPostStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateContent(String primaryLane, String desiredLane,
                              boolean hasMicrophone, String memo) {
        this.primaryLane = Lane.from(primaryLane);
        this.desiredLane = Lane.from(desiredLane);
        this.hasMicrophone = hasMicrophone;
        this.memo = memo;
        this.updatedAt = LocalDateTime.now();
    }
}
