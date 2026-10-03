package com.example.lolserver.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 운영에서는 Flyway 가 스키마를 적용하지 않고 검증만 한다.
 *
 * <p>마이그레이션 적용 주체는 lol-repository 다. 다만 lol-server 도 같은 lol-db-schema 를
 * 싣고 있으므로, 자기가 들고 있는 마이그레이션과 DB 적용 이력이 어긋나면 기동에서 잡아야
 * 한다. Boot 의 Flyway 오토컨피그는 기본적으로 {@code migrate()} 를 돌리고 그걸 끄는
 * 프로퍼티는 없으므로, 전략 빈으로 {@code validate()} 로 바꾼다.
 *
 * <p>검증이 잡아 주는 것과 못 잡는 것이 다르다. 이 검증은 <em>마이그레이션 이력</em>을 본다
 * (체크섬 불일치, DB 에 적용됐는데 로컬에 없는 마이그레이션). 엔티티와 실제 테이블의
 * 불일치는 {@code ddl-auto: validate} 가 따로 본다 — 둘은 대체 관계가 아니다.
 *
 * <p><b>배포 순서가 묶인다.</b> 빌드에만 있고 DB 에 아직 적용되지 않은 마이그레이션(pending)
 * 이 있으면 validate 가 실패한다. 즉 새 마이그레이션이 생기면 lol-repository 가 먼저
 * 적용돼야 lol-server 가 뜬다. 의도된 제약이다 — 스키마가 준비되지 않은 채 뜨는 것보다
 * 기동에서 멈추는 쪽이 낫다.
 *
 * <p>local 에는 이 빈이 없다({@code @Profile}). 빈 DB 에 lol-server 만 띄워도 스키마가
 * 만들어지도록 기본 전략(migrate)을 그대로 쓴다. 배경은 ADR 0003.
 */
@Configuration
@Profile("prod")
public class FlywayValidateOnlyConfig {

    @Bean
    public FlywayMigrationStrategy flywayValidateOnlyStrategy() {
        return Flyway::validate;
    }
}
