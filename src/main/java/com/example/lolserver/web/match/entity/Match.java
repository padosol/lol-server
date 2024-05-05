package com.example.lolserver.web.match.entity;

import com.example.lolserver.web.dto.data.gameData.GameInfoData;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    private String matchId;

    private String endOfGameResult;
    private String dateVersion;
    private	long gameCreation;
    private	long gameDuration;
    private	long gameEndTimestamp;
    private	long gameId;
    private	String gameMode;
    private	String gameName;
    private	long gameStartTimestamp;
    private	String gameType;
    private	String gameVersion;
    private	int mapId;
    private	String platformId;
    private	int queueId;
    private	String tournamentCode;

    private LocalDateTime gameCreationDateTime;
    private LocalDateTime gameEndDateTime;
    private LocalDateTime gameStartDateTime;

    @OneToMany(mappedBy = "match")
    private List<MatchSummoner> matchSummoners = new ArrayList<>();

    public void convertEpochToLocalDateTime() {
        this.gameCreationDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(gameCreation), ZoneOffset.UTC);
        this.gameEndDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(gameEndTimestamp), ZoneOffset.UTC);
        this.gameStartDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(gameStartTimestamp), ZoneOffset.UTC);
    }



}
