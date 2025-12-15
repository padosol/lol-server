package com.example.lolserver.repository.dto.data;

import com.example.lolserver.repository.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.repository.dto.error.ErrorData;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LeagueData extends ErrorData {

    private List<LeagueSummonerData> leagues;
    public LeagueData(){};
    public LeagueData(boolean notFound) {
        super(notFound);
    }
}
