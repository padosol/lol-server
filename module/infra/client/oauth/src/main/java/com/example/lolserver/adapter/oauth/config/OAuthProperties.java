package com.example.lolserver.adapter.oauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private Map<String, ProviderConfig> providers = new HashMap<>();

    public ProviderConfig getProviderConfig(String providerName) {
        ProviderConfig config = providers.get(providerName.toLowerCase());
        if (config == null) {
            throw new IllegalArgumentException(
                    "OAuth provider config not found: " + providerName);
        }
        return config;
    }

    // 하위 호환용 — 기존 riot 필드 접근자 유지
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
