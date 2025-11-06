package com.example.lolserver.domain.summoner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class SummonerRequest {
    @Schema(description = "유저네임 ex) hideonbush-kr1", defaultValue = "hideonbush-kr1")
    private String q;
    @Schema(description = "플랫폼", defaultValue = "kr")
    private String region;
}
