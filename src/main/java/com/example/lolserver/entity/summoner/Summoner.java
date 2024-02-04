package com.example.lolserver.entity.summoner;


import com.example.lolserver.web.dto.data.SummonerData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Summoner {

    @Id
    @Column(name = "summoner_id")
    private String id;
    private String accountId;
    private String puuid;
    private String name;
    @Column(name = "profile_icon_id")
    private int profileIconId;
    private long revisionDate;
    private long summonerLevel;

    private String gameName;
    private String tagLine;


    public SummonerData toData() {
        return SummonerData.builder()
                .summonerId(id)
                .accountId(accountId)
                .name(name)
                .profileIconId(profileIconId)
                .puuid(puuid)
                .revisionDate(revisionDate)
                .summonerLevel(summonerLevel)
                .gameName(gameName)
                .tagLine(tagLine)
                .build();
    }

}
