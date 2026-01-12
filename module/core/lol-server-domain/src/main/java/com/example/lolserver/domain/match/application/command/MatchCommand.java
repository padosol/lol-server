package com.example.lolserver.domain.match.application.command;

import lombok.*;

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
