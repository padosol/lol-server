package com.example.lolserver.web.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequest {
    private String puuid;
    @Schema(description = "Queue Type", defaultValue = "420")
    private Integer queueId;
    @Schema(description = "페이지 No", defaultValue = "0")
    private Integer pageNo;
    @Schema(description = "지역", defaultValue = "KR")
    private String region;
}
