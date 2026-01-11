package com.example.lolserver.repository.rank.entity;

import com.example.lolserver.Division;
import com.example.lolserver.QueueType;
import com.example.lolserver.Tier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ranks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private QueueType queueType;
    private String summonerName;
    private String tagLine;
    private String summonerId;
    private String leagueId;
    private int win;
    private int losses;
    private int point;
    @Enumerated(EnumType.STRING)
    private Tier tier;
    @Enumerated(EnumType.STRING)
    private Division division;
    private String puuid;
    private long summonerLevel;
    private String position;
    // For simplicity, championNames will be stored as a comma-separated string
    private String championNames;
}
