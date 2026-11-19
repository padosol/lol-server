package com.example.lolserver.esports.domain.vo;

/** 인물의 현재 e스포츠 상태. 역할(선수/감독/코치)은 상태가 아니라 {@link Role} 이다. */
public enum PersonStatus {
    CONTRACTED,
    ON_LOAN,
    FREE_AGENT,
    RETIRED
}
