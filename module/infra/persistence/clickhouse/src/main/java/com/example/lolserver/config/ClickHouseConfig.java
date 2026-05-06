package com.example.lolserver.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

// TODO(MP-9): ChampionStatsBigQueryAdapter 가 @Primary 로 활성화된 이후 ClickHouse adapter 는 dead code at runtime.
//             ClickHouseConfig + ChampionStatsClickHouseAdapter + clickhouse 모듈 의존성 정리는 별도 cleanup PR 로 트래킹.
@Configuration
public class ClickHouseConfig {

    @Bean("clickHouseJdbcTemplate")
    public JdbcTemplate clickHouseJdbcTemplate(
            @Value("${clickhouse.datasource.url}") String url,
            @Value("${clickhouse.datasource.driver-class-name}") String driverClassName,
            @Value("${clickhouse.datasource.username}") String username,
            @Value("${clickhouse.datasource.password}") String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(10);
        return new JdbcTemplate(dataSource);
    }
}
