package com.example.lolserver.gamedata.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
     * 응답을 내려주는데, 이런 값은 짧은 TTL(negative cache)로만 저장하기 위해 판별하는 guard.
     *
     * <p>{@code @JsonIgnore} 필수: Jackson 은 {@code isEmpty()} 를 {@code "empty"} 프로퍼티로
     * 인식해 Redis 캐시 JSON 에 직렬화하는데, 이 필드는 역직렬화 대상(필드/생성자 인자)이 아니라
     * 읽을 때 "Unrecognized field empty" 로 캐시 조회가 전량 실패한다. 파생 값이므로 직렬화에서 제외한다.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return freeChampionIds == null || freeChampionIds.isEmpty();
    }
}
