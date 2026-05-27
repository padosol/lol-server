package com.example.lolserver.match.application.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 블루/레드 팀 정보 읽기 모델. 필드명은 캐시/API 계약이므로 변경 금지.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamReadModel {
    private TeamInfoReadModel blueTeam;
    private TeamInfoReadModel redTeam;
}
