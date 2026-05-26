package com.example.lolserver.common.test;

import com.example.lolserver.common.config.AsyncQueryConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// @DataJpaTest 슬라이스용 공유 설정. 기본 스테레오타입 스캔(@Component 등)을 유지하되,
// 영속성 어댑터(*.adapter.out.persistence.*: 어댑터 + MapStruct 매퍼)만 남기고
// 나머지 레이어(application 서비스, adapter.in 컨트롤러, client/messaging/cache driven 어댑터, *.config)는
// 제외한다 — 이들은 RestClient/RabbitTemplate/타 포트 등 JPA 슬라이스에 없는 빈을 요구하기 때문.
// 엔티티/리포지토리는 @EntityScan/@EnableJpaRepositories, JPA 보조 빈은 @Import 로 주입.
@SpringBootApplication
@EntityScan(basePackages = "com.example.lolserver")
@EnableJpaRepositories(basePackages = "com.example.lolserver")
@ComponentScan(
        basePackages = "com.example.lolserver",
        excludeFilters = {
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.config\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.application\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.adapter\\.in\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.adapter\\.out\\.client\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.adapter\\.out\\.messaging\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.adapter\\.out\\.cache\\..*")
        })
@Import(AsyncQueryConfig.class)
public class TestJpaConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
