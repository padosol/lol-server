package com.example.lolserver.web.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MatchRequest {
    private String puuid;
    private Integer queueId;
    private Integer pageNo;
}
