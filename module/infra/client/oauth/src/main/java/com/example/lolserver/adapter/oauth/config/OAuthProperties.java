package com.example.lolserver.adapter.oauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private ProviderConfig google = new ProviderConfig();
    private ProviderConfig riot = new ProviderConfig();

    @Getter
    @Setter
    public static class ProviderConfig {
        private String clientId;
        private String clientSecret;
        private String tokenUri;
        private String userInfoUri;
        private String accountUri;
        private String authorizationUri;
        private String scope;
        private String callbackUri;
    }
}
