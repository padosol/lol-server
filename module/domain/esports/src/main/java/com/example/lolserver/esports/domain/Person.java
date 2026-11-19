package com.example.lolserver.esports.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 인물 마스터. 프로 선수·감독·코치·크리에이터 공통이다. 역할과 소속은 계약이 가진다.
 * ID 는 사람이 읽는 고정 키(faker)이고 바꾸지 않는다. 활동명은 중복될 수 있다.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Person {

    private String personId;
    private String nickName;
    private String realName;
    private String realNameEng;
    private LocalDate birthDate;
    private String nationality;
    private String imageUrl;
}
