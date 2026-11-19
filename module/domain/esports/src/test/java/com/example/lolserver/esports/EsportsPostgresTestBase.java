package com.example.lolserver.esports;

import com.example.lolserver.common.config.QueryDslConfig;
import com.example.lolserver.common.test.TestJpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 Postgres 에 app Flyway 체인을 적용한 JPA 슬라이스.
 *
 * <p>로스터는 Postgres 전용 기능(EXCLUDE USING gist, daterange, 뷰)에 기대므로 다른 모듈처럼 H2 를 쓰지 않는다.
 * 마이그레이션은 app 모듈의 {@code db/migration/app} 을 그대로 쓴다(build.gradle 의 {@code esports.appMigrationDir}).
 * 뷰는 {@code CURRENT_DATE} 기준이라 픽스처는 {@code CURRENT_DATE ± n} 으로 만든다.
 */
@DataJpaTest
@ContextConfiguration(classes = TestJpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class EsportsPostgresTestBase {

    /**
     * JVM 당 한 번만 띄운다. 클래스마다 {@code @Container} 로 띄우면 첫 클래스가 끝날 때 컨테이너가 내려가는데,
     * Spring 이 캐시한 컨텍스트는 내려간 컨테이너 주소를 계속 써서 다음 클래스가 연결에 실패한다.
     * Docker 가 없으면 {@code disabledWithoutDocker} 조건이 클래스 초기화 전에 건너뛰게 한다. 종료는 Ryuk 이 맡는다.
     */
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl() + "&currentSchema=app");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "filesystem:" + System.getProperty("esports.appMigrationDir"));
        registry.add("spring.flyway.schemas", () -> "app");
        registry.add("spring.flyway.default-schema", () -> "app");
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    /** 같은 트랜잭션 안에서 SQL 을 실행한다 (테스트 끝에 롤백된다). */
    protected void sql(String script) {
        jdbcTemplate.execute(script);
    }
}
