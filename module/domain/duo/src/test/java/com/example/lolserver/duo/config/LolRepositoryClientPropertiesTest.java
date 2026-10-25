package com.example.lolserver.duo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LolRepositoryClientPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @DisplayName("http(s) 주소면 바인딩된다")
    @ParameterizedTest
    @ValueSource(strings = {"http://lol-repository:8081", "http://localhost:8112", "https://repo.internal"})
    void validUrl_binds(String url) {
        contextRunner.withPropertyValues("lol.repository.internal-url=" + url)
                .run(context -> assertThat(context.getBean(LolRepositoryClientProperties.class).getInternalUrl())
                        .isEqualTo(url));
    }

    @DisplayName("환경변수가 빠져 ${...} 가 그대로 남으면 기동이 실패한다")
    @Test
    void unresolvedPlaceholder_failsStartup() {
        contextRunner.withPropertyValues("lol.repository.internal-url=${LOL_REPOSITORY_INTERNAL_URL}")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("LOL_REPOSITORY_INTERNAL_URL"));
    }

    @DisplayName("값이 없으면 기동이 실패한다")
    @Test
    void missing_failsStartup() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LolRepositoryClientProperties.class)
    static class PropertiesConfig {
    }
}
