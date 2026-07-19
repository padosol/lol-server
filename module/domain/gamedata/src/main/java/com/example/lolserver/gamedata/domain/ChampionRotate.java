package com.example.lolserver.gamedata.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ChampionRotate {

    private int maxNewPlayerLevel;
    private List<Integer> freeChampionIdsForNewPlayers;
    private List<Integer> freeChampionIds;

    /**
     * 로테이션 데이터가 비어 있는지 여부.
     * upstream(lol-repository)이 Riot 조회에 실패하면 {@code freeChampionIds} 가 null/빈 값인
     * 응답을 내려주는데, 이런 값은 캐싱하면 안 되므로 이를 판별하는 guard.
     */
    public boolean isEmpty() {
        return freeChampionIds == null || freeChampionIds.isEmpty();
    }
}
