package com.example.lolserver.domain.match.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCommand {
    private String puuid;
    private Integer queueId;
    private Integer pageNo;
    private String region;
}
