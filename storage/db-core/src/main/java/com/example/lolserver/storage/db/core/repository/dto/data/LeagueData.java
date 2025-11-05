package com.example.lolserver.storage.db.core.repository.dto.data;

import com.example.lolserver.storage.db.core.repository.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.storage.db.core.repository.dto.error.ErrorData;
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
