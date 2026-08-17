package com.example.lolserver.member.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 프로바이더별 부가 API 엔드포인트 설정.
 *
 * <p>인증(authorization/token 교환)에 필요한 값은 Spring Security 표준 설정
 * (`spring.security.oauth2.client.*`) 이 관리한다. 여기에는 표준 설정이 다루지 않는
 * 부가 조회 엔드포인트만 둔다 — 현재는 Riot PUUID 조회용 account-uri 뿐이다.
 */
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

    @Getter
    @Setter
    public static class ProviderConfig {
        private String accountUri;
    }
}
