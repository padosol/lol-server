package com.example.lolserver.domain.member.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiotAccountLink {

    private Long id;
    private Long memberId;
    private String puuid;
    private String gameName;
    private String tagLine;
    private String platformId;
    private LocalDateTime linkedAt;
}
