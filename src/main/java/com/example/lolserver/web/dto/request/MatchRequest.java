package com.example.lolserver.web.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchRequest {
    private String puuid;
    private Integer queueId;
    private Integer pageNo;
}
