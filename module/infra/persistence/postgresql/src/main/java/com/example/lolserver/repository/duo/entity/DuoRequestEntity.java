package com.example.lolserver.repository.duo.entity;

import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.repository.duo.converter.MostChampionListConverter;
import com.example.lolserver.repository.duo.converter.RecentGameSummaryConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "duo_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuoRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "duo_post_id", nullable = false)
    private Long duoPostId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "requester_puuid", nullable = false, length = 78)
    private String requesterPuuid;

    @Column(name = "primary_lane", nullable = false, length = 20)
    private String primaryLane;

    @Column(name = "desired_lane", nullable = false, length = 20)
    private String desiredLane;

    @Column(name = "has_microphone", nullable = false)
    private boolean hasMicrophone;

    @Column(nullable = false, length = 20)
    private String tier;

    @Column(name = "tier_rank", nullable = false, length = 10)
    private String tierRank;

    @Column(name = "league_points", nullable = false)
    private int leaguePoints;

    @Column(length = 500)
    private String memo;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "most_champions", columnDefinition = "TEXT")
    @Convert(converter = MostChampionListConverter.class)
    private List<MostChampion> mostChampions;

    @Column(name = "recent_game_summary", columnDefinition = "TEXT")
    @Convert(converter = RecentGameSummaryConverter.class)
    private RecentGameSummary recentGameSummary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
