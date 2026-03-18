package com.example.lolserver.domain.member.application.dto;

import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
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
public class OAuthLoginCommand {

    private OAuthProvider provider;
    private String code;
    private String redirectUri;
    private String state;
}
