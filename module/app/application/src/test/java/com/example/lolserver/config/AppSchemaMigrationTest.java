package com.example.lolserver.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * app 스키마 Flyway 체인(db/migration/app)을 실제 Postgres 에서 검증한다.
 * Flyway 설정은 postgresql-{local,prod}.yml 의 spring.flyway 를 그대로 읽어 쓴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class AppSchemaMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String[] APP_ENTITY_PACKAGES = {
        "com.example.lolserver.member.adapter.out.persistence.entity",
        "com.example.lolserver.community.adapter.out.persistence.entity",
        "com.example.lolserver.duo.adapter.out.persistence.entity"
    };

    private static final int APP_TABLE_COUNT = 14;

    @BeforeEach
    void resetDatabase() throws SQLException {
        execute("DROP SCHEMA IF EXISTS app CASCADE; DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
    }

    @DisplayName("빈 DB 면 app 스키마를 만들고 V1 부터 적용하며, app 엔티티 검증을 통과한다")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"postgresql-local.yml", "postgresql-prod.yml"})
    void migratesEmptyDatabaseFromV1(String yaml) throws Exception {
        // when
        flyway(yaml).migrate();

        // then
        assertThat(appliedVersions()).containsExactly("1", "2", "3", "4");
        assertThat(appTableCount()).isEqualTo(APP_TABLE_COUNT);
        assertThat(queryInt("SELECT count(*) FROM app.community_category")).isEqualTo(8);
        validateAppEntities();
    }

    @DisplayName("public 에 예전 app 테이블이 남아 있어도 옮기지 않고 app 에 새로 만든다")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"postgresql-local.yml", "postgresql-prod.yml"})
    void createsAppSchemaBesideOldPublicTables(String yaml) throws Exception {
        // given — 운영(V35)·예전 로컬처럼 공유 체인(lol-db-schema)이 public 에 app 테이블을 만든 상태
        runScript("public", "V1__init_app_schema.sql");

        // when
        flyway(yaml).migrate();

        // then
        assertThat(appliedVersions()).containsExactly("1", "2", "3", "4");
        assertThat(appTableCount()).isEqualTo(APP_TABLE_COUNT);
        assertThat(tableExists("public", "member")).isTrue();
        validateAppEntities();
    }

    @DisplayName("app 스키마에 이력 없이 테이블만 있으면 V1 을 건너뛰지 않고 실패한다 (baseline 하지 않음)")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"postgresql-local.yml", "postgresql-prod.yml"})
    void refusesTablesWithoutHistory(String yaml) throws Exception {
        // given
        execute("CREATE SCHEMA app;");
        runScript("app", "V1__init_app_schema.sql");

        // when & then
        assertThatThrownBy(() -> flyway(yaml).migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("no schema history table");
        assertThat(tableExists("app", "flyway_schema_history")).isFalse();
    }

    @DisplayName("prod 는 접속 계정 그대로 마이그레이션한다 — SET ROLE 같은 별도 역할을 전제하지 않는다")
    @Test
    void prodMigratesAsConnectingAccount() throws Exception {
        assertThat(flywayProperties("postgresql-prod.yml").getInitSqls()).isEmpty();
    }

    private static FlywayProperties flywayProperties(String yaml) throws IOException {
        var sources = new YamlPropertySourceLoader().load(yaml, new ClassPathResource(yaml));
        return new Binder(ConfigurationPropertySources.from(sources))
                .bind("spring.flyway", FlywayProperties.class)
                .orElseThrow(() -> new IllegalStateException(yaml + " 에 spring.flyway 가 없다"));
    }

    private static Flyway flyway(String yaml) throws IOException {
        FlywayProperties props = flywayProperties(yaml);
        assertThat(props.isEnabled()).isTrue();
        var config = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(props.getLocations().toArray(String[]::new))
                .schemas(props.getSchemas().toArray(String[]::new))
                .defaultSchema(props.getDefaultSchema())
                .createSchemas(props.isCreateSchemas())
                .baselineOnMigrate(props.isBaselineOnMigrate())
                .baselineVersion(props.getBaselineVersion());
        // Spring Boot FlywayAutoConfiguration 과 같은 방식: 비어 있지 않으면 줄바꿈으로 이어 붙인다
        if (!props.getInitSqls().isEmpty()) {
            // user 가 없으면 Flyway 가 앱 커넥션 풀을 빌려 쓰고, init-sqls(SET ROLE)가 그 풀에 남는다
            assertThat(props.getUser()).as(yaml + ": init-sqls 를 쓰면 spring.flyway.user 로 전용 커넥션을 써야 한다")
                    .isNotNull();
            config.initSql(String.join("\n", props.getInitSqls()));
        }
        return config.load();
    }

    /** 운영과 같은 search_path·네이밍 전략으로 app 엔티티만 ddl-auto=validate 한다. 불일치면 예외. */
    private static void validateAppEntities() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl() + "&currentSchema=app",
                POSTGRES.getUsername(), POSTGRES.getPassword());
        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(APP_ENTITY_PACKAGES);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", "validate",
                "hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy.class.getName(),
                "hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName()));
        emf.afterPropertiesSet();
        emf.destroy();
    }

    private static void runScript(String schema, String migration) throws Exception {
        String sql = new ClassPathResource("db/migration/app/" + migration)
                .getContentAsString(StandardCharsets.UTF_8);
        execute("SET search_path TO " + schema + ";\n" + sql);
    }

    private static void execute(String sql) throws SQLException {
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private static int queryInt(String sql) throws SQLException {
        try (Connection conn = connect(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static boolean tableExists(String schema, String table) throws SQLException {
        return queryInt("SELECT count(*) FROM pg_tables WHERE schemaname = '" + schema
                + "' AND tablename = '" + table + "'") == 1;
    }

    private static int appTableCount() throws SQLException {
        return queryInt("SELECT count(*) FROM pg_tables WHERE schemaname = 'app' AND tablename <> 'flyway_schema_history'");
    }

    /** app.flyway_schema_history 에 성공으로 남은 버전. 스키마 생성 표시(version 없음)는 제외. */
    private static List<String> appliedVersions() throws SQLException {
        List<String> versions = new ArrayList<>();
        try (Connection conn = connect(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM app.flyway_schema_history"
                     + " WHERE version IS NOT NULL AND success ORDER BY installed_rank")) {
            while (rs.next()) {
                versions.add(rs.getString(1));
            }
        }
        return versions;
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
