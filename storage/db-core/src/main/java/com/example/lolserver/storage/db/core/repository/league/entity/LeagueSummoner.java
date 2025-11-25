package com.example.lolserver.storage.db.core.repository.league.entity;


import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.storage.db.core.repository.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.storage.db.core.repository.league.entity.id.LeagueSummonerId;
import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "league_summoner")
public class LeagueSummoner {

    @Id
    @Column(name = "league_summoner_id")
    private Long id;

    @Column(name = "league_id")
    private String leagueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "league_id",
            referencedColumnName = "league_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private League league;

    private String puuid;

    public LeagueSummonerData toData() {
        return LeagueSummonerData.builder()
                .build();
    }

}
