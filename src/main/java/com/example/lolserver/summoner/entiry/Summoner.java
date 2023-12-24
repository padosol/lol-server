package com.example.lolserver.summoner.entiry;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Summoner {

    @Id
    private String accountId;
    private int profileIconId;
    private long revisionDate;
    private String name;
    private String id;
    private String puuid;
    private long summonerLevel;

}
