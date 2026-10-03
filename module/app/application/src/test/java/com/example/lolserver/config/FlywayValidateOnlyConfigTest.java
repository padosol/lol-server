package com.example.lolserver.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FlywayValidateOnlyConfigTest {

    @Mock
    private Flyway flyway;

    @Nested
    @DisplayName("flywayValidateOnlyStrategy")
    class ValidateOnlyStrategy {

        @DisplayName("DB 에 쓰지 않는다 — migrate 대신 validate 를 호출한다")
        @Test
        void validatesWithoutMigrating() {
            // given
            FlywayMigrationStrategy strategy = new FlywayValidateOnlyConfig().flywayValidateOnlyStrategy();

            // when
            strategy.migrate(flyway);

            // then
            then(flyway).should().validate();
            then(flyway).should(never()).migrate();
        }
    }

    @Nested
    @DisplayName("프로파일")
    class Profiles {

        // 이 애너테이션이 빠지면 local 까지 validate 로 바뀌어 빈 DB 개발이 막히고,
        // 반대로 클래스가 사라지면 운영이 조용히 migrate 로 돌아간다. 둘 다 조용한 사고라 고정한다.
        @DisplayName("prod 에만 적용된다 — 나머지 프로파일은 기본 전략(migrate)을 쓴다")
        @Test
        void onlyActiveOnProd() {
            // when
            Profile profile = FlywayValidateOnlyConfig.class.getAnnotation(Profile.class);

            // then
            assertThat(profile).isNotNull();
            assertThat(profile.value()).containsExactly("prod");
        }
    }
}
