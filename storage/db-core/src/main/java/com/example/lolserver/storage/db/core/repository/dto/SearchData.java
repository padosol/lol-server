package com.example.lolserver.storage.db.core.repository.dto;

import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchData {

    // 소환사 정보
    private SummonerResponse summoner;
    
    // 리그정보

    // 최근게임 20개

    // 정보없음

    private boolean notFound;

    public SearchData() {};

    public SearchData(boolean notFound) {
        this.notFound = notFound;
    }

}
