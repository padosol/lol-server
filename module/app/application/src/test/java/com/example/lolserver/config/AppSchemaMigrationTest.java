package com.example.lolserver.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

    @Nested
    @DisplayName("local 설정")
    class Local {

        @DisplayName("빈 DB 면 app 스키마를 만들고 V1 부터 적용하며, app 엔티티 검증을 통과한다")
        @Test
        void migratesEmptyDatabaseFromV1() throws Exception {
            // when
            flyway("postgresql-local.yml").migrate();

            // then
            assertThat(appliedVersions()).containsExactly("1", "2");
            assertThat(appTableCount()).isEqualTo(APP_TABLE_COUNT);
            assertThat(queryInt("SELECT count(*) FROM app.community_category")).isEqualTo(8);
            validateAppEntities();
        }

        @DisplayName("lol-repository 가 public 에 app 테이블을 만든 로컬 DB 여도 app 에 새로 만들어 쓴다")
        @Test
        void createsAppSchemaBesidePublicCopies() throws Exception {
            // given — 공유 체인(lol-db-schema)이 public 에 app 테이블까지 만든 상태
            runScript("public", "V1__init_app_schema.sql");

            // when
            flyway("postgresql-local.yml").migrate();

            // then
            assertThat(appliedVersions()).containsExactly("1", "2");
            assertThat(tableExists("app", "member")).isTrue();
            validateAppEntities();
        }
    }

    @Nested
    @DisplayName("prod 설정")
    class Prod {

        @DisplayName("스키마 이동 전(app 스키마 없음)에는 실패하고 아무것도 만들지 않는다")
        @Test
        void failsBeforeSchemaMove() throws Exception {
            // given
            runScript("public", "V1__init_app_schema.sql");

            // when & then
            assertThatThrownBy(() -> flyway("postgresql-prod.yml").migrate())
                    .isInstanceOf(FlywayException.class);
            assertThat(schemaExists("app")).isFalse();
        }

        @DisplayName("app 만 옮긴 직후(이력 없음, Riot 테이블은 public 에 남음)면 V1 을 baseline 으로 기록하고 V2 만 적용한다")
        @Test
        void baselinesMovedSchemaAndAppliesV2() throws Exception {
            // given — 운영(V35) 상태의 app 테이블만 02-move-schema.sql 로 옮겨지고 Riot 테이블은 public 에 남은 상태
            execute("CREATE SCHEMA app;");
            runScript("app", "V1__init_app_schema.sql");
            execute("CREATE TABLE public.match (match_id VARCHAR(30) PRIMARY KEY);");

            // when
            flyway("postgresql-prod.yml").migrate();

            // then
            assertThat(historyTypes()).containsExactly(Map.entry("1", "BASELINE"), Map.entry("2", "SQL"));
            assertThat(appTableCount()).isEqualTo(APP_TABLE_COUNT);
            assertThat(tableExists("public", "flyway_schema_history")).isFalse();
            validateAppEntities();
        }

        @DisplayName("공유 체인으로 V36(community_image)까지 적용된 뒤 이동했어도 V2 가 실패하지 않는다")
        @Test
        void toleratesCommunityImageAlreadyPresent() throws Exception {
            // given
            execute("CREATE SCHEMA app;");
            runScript("app", "V1__init_app_schema.sql");
            runScript("app", "V2__add_community_image.sql");

            // when
            flyway("postgresql-prod.yml").migrate();

            // then
            assertThat(historyTypes()).containsExactly(Map.entry("1", "BASELINE"), Map.entry("2", "SQL"));
            validateAppEntities();
        }

        @DisplayName("app 스키마가 있어도 app 테이블이 public 에 남아 있으면 거부한다")
        @Test
        void refusesWhenPublicStillHasAppTables() throws Exception {
            // given
            execute("CREATE SCHEMA app;");
            runScript("public", "V1__init_app_schema.sql");

            // when & then
            assertThatThrownBy(() -> flyway("postgresql-prod.yml").migrate())
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("02-move-schema.sql");
        }
    }

    private static Flyway flyway(String yaml) throws IOException {
        var sources = new YamlPropertySourceLoader().load(yaml, new ClassPathResource(yaml));
        FlywayProperties props = new Binder(ConfigurationPropertySources.from(sources))
                .bind("spring.flyway", FlywayProperties.class)
                .orElseThrow(() -> new IllegalStateException(yaml + " 에 spring.flyway 가 없다"));
        assertThat(props.isEnabled()).isTrue();
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(props.getLocations().toArray(String[]::new))
                .schemas(props.getSchemas().toArray(String[]::new))
                .defaultSchema(props.getDefaultSchema())
                .createSchemas(props.isCreateSchemas())
                .baselineOnMigrate(props.isBaselineOnMigrate())
                .baselineVersion(props.getBaselineVersion())
                .load();
    }

    /** 운영과 같은 search_path·네이밍 전략으로 app 엔티티만 ddl-auto=validate 한다. 불일치면 예외. */
    private static void validateAppEntities() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl() + "&currentSchema=app,riot,public",
                POSTGRES.getUsername(), POSTGRES.getPassword());
        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(APP_ENTITY_PACKAGES);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", "validate",
                "hibernate.hbm2ddl.jdbc_metadata_extraction_strategy", "individually",
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

    private static boolean schemaExists(String schema) throws SQLException {
        return queryInt("SELECT count(*) FROM pg_namespace WHERE nspname = '" + schema + "'") == 1;
    }

    private static boolean tableExists(String schema, String table) throws SQLException {
        return queryInt("SELECT count(*) FROM pg_tables WHERE schemaname = '" + schema
                + "' AND tablename = '" + table + "'") == 1;
    }

    private static int appTableCount() throws SQLException {
        return queryInt("SELECT count(*) FROM pg_tables WHERE schemaname = 'app' AND tablename <> 'flyway_schema_history'");
    }

    private static List<String> appliedVersions() throws SQLException {
        List<String> versions = new ArrayList<>();
        for (Map.Entry<String, String> e : historyTypes()) {
            versions.add(e.getKey());
        }
        return versions;
    }

    /** app.flyway_schema_history 의 (version, type). 스키마 생성 표시(version 없음)는 제외. */
    private static List<Map.Entry<String, String>> historyTypes() throws SQLException {
        List<Map.Entry<String, String>> rows = new ArrayList<>();
        try (Connection conn = connect(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT version, type FROM app.flyway_schema_history"
                     + " WHERE version IS NOT NULL AND success ORDER BY installed_rank")) {
            while (rs.next()) {
                rows.add(Map.entry(rs.getString(1), rs.getString(2)));
            }
        }
        return rows;
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
