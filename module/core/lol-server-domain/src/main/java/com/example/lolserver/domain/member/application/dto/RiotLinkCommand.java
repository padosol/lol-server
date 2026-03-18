package com.example.lolserver.domain.member.application.dto;

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
public class RiotLinkCommand {

    private String code;
    private String redirectUri;
    private String platformId;
}
