package com.example.lolserver.esports.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 소환사(라이엇 계정) → 인물 연결. puuid 만 저장하고 소환사 정보는 화면이 lol-repository 에서 받는다.
 * 한 계정은 한 사람에게만 속하고, 대표 계정은 인물당 하나다.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonSummoner {

    private String puuid;
    private String personId;
    private boolean main;
    private boolean visible;
    private String note;
}
