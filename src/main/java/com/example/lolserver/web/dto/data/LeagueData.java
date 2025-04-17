package com.example.lolserver.web.dto.data;

import java.util.List;

import com.example.lolserver.web.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.web.dto.error.ErrorData;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeagueData extends ErrorData {

    private List<LeagueSummonerData> leagues;
    public LeagueData(){};
    public LeagueData(boolean notFound) {
        super(notFound);
    }
}
