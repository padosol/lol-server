package com.example.lolserver.duo.domain;

import com.example.lolserver.duo.domain.vo.DuoRequestStatus;
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
public class DuoRequest {

    private Long id;
    private Long duoPostId;
    private Long requesterId;
    private String requesterPuuid;
    private Lane primaryLane;
    private Lane desiredLane;
    private boolean hasMicrophone;
    private String tier;
    private String rank;
    private int leaguePoints;
    private String memo;
    private DuoRequestStatus status;
    private List<MostChampion> mostChampions;
    private RecentGameSummary recentGameSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DuoRequest create(Long duoPostId, Long requesterId,
            String requesterPuuid, Lane primaryLane, Lane desiredLane,
            boolean hasMicrophone, String memo, RiotAccountStats stats) {
        TierInfo tierInfo = stats.tierInfo();
        tierInfo.validateRanked();

        LocalDateTime now = LocalDateTime.now();
        return DuoRequest.builder()
                .duoPostId(duoPostId)
                .requesterId(requesterId)
                .requesterPuuid(requesterPuuid)
                .primaryLane(primaryLane)
                .desiredLane(desiredLane)
                .hasMicrophone(hasMicrophone)
                .tier(tierInfo.tier())
                .rank(tierInfo.rank())
                .leaguePoints(tierInfo.leaguePoints())
                .memo(memo)
                .status(DuoRequestStatus.PENDING)
                .mostChampions(stats.mostChampions())
                .recentGameSummary(stats.recentGameSummary())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void validateRequester(Long memberId) {
        if (!this.requesterId.equals(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }
    }

    public void accept() {
        if (this.status != DuoRequestStatus.PENDING) {
            throw new CoreException(ErrorType.DUO_REQUEST_NOT_PENDING);
        }
        this.status = DuoRequestStatus.ACCEPTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirm() {
        if (this.status != DuoRequestStatus.ACCEPTED) {
            throw new CoreException(ErrorType.DUO_REQUEST_NOT_ACCEPTED);
        }
        this.status = DuoRequestStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject() {
        validateOpen();
        this.status = DuoRequestStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        validateOpen();
        this.status = DuoRequestStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        validateOpen();
        this.status = DuoRequestStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateOpen() {
        if (this.status != DuoRequestStatus.PENDING
                && this.status != DuoRequestStatus.ACCEPTED) {
            throw new CoreException(ErrorType.DUO_REQUEST_ALREADY_COMPLETED);
        }
    }
}
