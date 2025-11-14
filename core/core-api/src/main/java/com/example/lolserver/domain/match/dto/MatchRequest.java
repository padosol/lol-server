package com.example.lolserver.domain.match.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequest {
    private String puuid;
    private Integer queueId;
    private Integer pageNo;
    private String region;
}
