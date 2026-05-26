package com.example.lolserver.match.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 팀 단건 정보 읽기 모델. 필드명은 캐시/API 계약이므로 변경 금지.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamInfoReadModel {

    private int teamId;
    private boolean win;

    private int championKills;

    private int baronKills;
    private int dragonKills;
    private int towerKills;
    private int inhibitorKills;

    private Integer[] goldTimeline;
    private Integer[] timestamps;
}
