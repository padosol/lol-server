package com.example.lolserver.adapter.oauth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuthTokenResponse {

    private String accessToken;
    private String refreshToken;
    private String idToken;
    private String tokenType;
    private Integer expiresIn;
    private String sub;

    public static OAuthTokenResponse from(Map<String, Object> response) {
        return OAuthTokenResponse.builder()
                .accessToken((String) response.get("access_token"))
                .refreshToken((String) response.get("refresh_token"))
                .idToken((String) response.get("id_token"))
                .tokenType((String) response.get("token_type"))
                .expiresIn(response.get("expires_in") instanceof Number n ? n.intValue() : null)
                .sub((String) response.get("sub"))
                .build();
    }
}
