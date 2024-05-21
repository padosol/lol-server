package com.example.lolserver.web.match.dto;

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
    private String platform;
}
