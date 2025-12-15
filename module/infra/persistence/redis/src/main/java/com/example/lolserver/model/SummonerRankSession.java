package com.example.lolserver.model;

import com.example.lolserver.type.Division;
import com.example.lolserver.type.Tier;
import com.example.lolserver.storage.db.core.repository.league.entity.QueueType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@RedisHash
@NoArgsConstructor
@AllArgsConstructor
public class SummonerRankSession implements Serializable {

    private QueueType queueType;
    private String summonerName;
    private String tagLine;
    private String summonerId;
    private String leagueId;
    private int win;
    private int losses;
    private int point;
    private Tier tier;
    private Division division;
    private String puuid;

    private long summonerLevel;

    private String position;
    private List<String> championNames;

    private Double score;
    private String key;

    public boolean hasKey() {
        return StringUtils.hasText(this.key);
    }

}