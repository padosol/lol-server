package com.example.lolserver.redis.model;

import com.example.lolserver.riot.type.Division;
import com.example.lolserver.riot.type.Tier;
import com.example.lolserver.web.league.entity.League;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.entity.QueueType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@RedisHash
@NoArgsConstructor
@AllArgsConstructor
public class SummonerRankSession implements Serializable {

    private QueueType queueType;
    private String summonerName;
    private String summonerId;
    private String leagueId;
    private int win;
    private int losses;
    private int point;
    private Tier tier;
    private Division division;

    private Double score;
    private String key;

    public SummonerRankSession(League league, LeagueSummoner leagueSummoner) {
        this.queueType = league.getQueue();
        this.tier = Tier.valueOf(league.getTier());

        this.summonerName = leagueSummoner.getSummoner().getGameName() + "#" + leagueSummoner.getSummoner().getTagLine();

        this.leagueId = league.getLeagueId();
        this.summonerId = leagueSummoner.getSummoner().getId();

        this.win = leagueSummoner.getWins();
        this.losses = leagueSummoner.getLosses();
        this.point = leagueSummoner.getLeaguePoints();
        this.division = Division.valueOf(leagueSummoner.getRank());

        if(QueueType.RANKED_SOLO_5x5.equals(league.getQueue())) {
            this.key = "solo";
        } else if(QueueType.RANKED_FLEX_SR.equals(league.getQueue())) {
            this.key = "flex";
        }

        this.score = -(double) (this.point + this.tier.getScore() + this.division.getScore());

    }

    public Double getScore() {
        return this.score;
    }

    public boolean hasKey() {
        return StringUtils.hasText(this.key);
    }

}
