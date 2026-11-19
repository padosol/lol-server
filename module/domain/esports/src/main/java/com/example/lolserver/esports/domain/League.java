package com.example.lolserver.esports.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 리그·국제 대회 마스터 (lck, lpl, worlds). ID 는 운영자가 정하고 바꾸지 않는다. */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class League {

    private String leagueId;
    private String gameCode;
    private String name;
    private String nameAcronym;
    private String region;
    private boolean international;
    private String imageUrl;
    private String darkImageUrl;
    private int sortOrder;
    private boolean active;
}
