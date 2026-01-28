package com.example.lolserver.adapter.champion;

import com.example.lolserver.domain.champion.domain.ChampionRotate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeChampionClientAdapterTest {

    @DisplayName("Fake 챔피언 로테이션 응답 테스트")
    @Nested
    class GetChampionRotateTest {

        @DisplayName("Fake 응답에는 고정된 챔피언 데이터가 포함된다")
        @Test
        void getChampionRotate_returnsFixedFakeData() {
            // given
            FakeChampionClientProperties properties = createDefaultProperties();
            properties.setDelayMs(0); // 테스트 속도를 위해 지연 제거
            FakeChampionClientAdapter adapter = new FakeChampionClientAdapter(properties);

            // when
            ChampionRotate result = adapter.getChampionRotate("kr");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMaxNewPlayerLevel()).isEqualTo(10);
            assertThat(result.getFreeChampionIds()).hasSize(15);
            assertThat(result.getFreeChampionIdsForNewPlayers()).hasSize(10);
        }

        @DisplayName("지역에 관계없이 동일한 Fake 응답을 반환한다")
        @Test
        void getChampionRotate_anyRegion_returnsSameResponse() {
            // given
            FakeChampionClientProperties properties = createDefaultProperties();
            properties.setDelayMs(0);
            FakeChampionClientAdapter adapter = new FakeChampionClientAdapter(properties);

            // when
            ChampionRotate krResult = adapter.getChampionRotate("kr");
            ChampionRotate naResult = adapter.getChampionRotate("na1");

            // then
            assertThat(krResult.getFreeChampionIds())
                    .isEqualTo(naResult.getFreeChampionIds());
            assertThat(krResult.getFreeChampionIdsForNewPlayers())
                    .isEqualTo(naResult.getFreeChampionIdsForNewPlayers());
        }
    }

    @DisplayName("응답 지연 테스트")
    @Nested
    class DelayTest {

        @DisplayName("설정된 시간만큼 응답이 지연된다")
        @Test
        void getChampionRotate_withDelay_delaysResponse() {
            // given
            FakeChampionClientProperties properties = createDefaultProperties();
            properties.setDelayMs(200);
            properties.setRateLimitRequests(1000); // rate limit 영향 제거
            FakeChampionClientAdapter adapter = new FakeChampionClientAdapter(properties);

            // when
            long startTime = System.currentTimeMillis();
            adapter.getChampionRotate("kr");
            long elapsedTime = System.currentTimeMillis() - startTime;

            // then
            assertThat(elapsedTime).isGreaterThanOrEqualTo(200);
        }

        @DisplayName("지연 시간이 0이면 즉시 응답한다")
        @Test
        void getChampionRotate_noDelay_respondsImmediately() {
            // given
            FakeChampionClientProperties properties = createDefaultProperties();
            properties.setDelayMs(0);
            FakeChampionClientAdapter adapter = new FakeChampionClientAdapter(properties);

            // when
            long startTime = System.currentTimeMillis();
            adapter.getChampionRotate("kr");
            long elapsedTime = System.currentTimeMillis() - startTime;

            // then
            assertThat(elapsedTime).isLessThan(100);
        }
    }

    @DisplayName("Rate Limit 테스트")
    @Nested
    class RateLimitTest {

        @DisplayName("Rate limit 초과 시 THROW_EXCEPTION 전략은 예외를 발생시킨다")
        @Test
        void getChampionRotate_rateLimitExceeded_throwsException() {
            // given
            FakeChampionClientProperties properties = createDefaultProperties();
            properties.setDelayMs(0);
            properties.setRateLimitRequests(2);
            properties.setRateLimitSeconds(10);
            properties.setRateLimitStrategy(FakeChampionClientProperties.RateLimitStrategy.THROW_EXCEPTION);
            FakeChampionClientAdapter adapter = new FakeChampionClientAdapter(properties);

            // when - 2개 요청은 성공
            adapter.getChampionRotate("kr");
            adapter.getChampionRotate("kr");

            // then - 3번째 요청은 예외 발생
            assertThatThrownBy(() -> adapter.getChampionRotate("kr"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("Rate limit exceeded");
        }

        @DisplayName("Rate limit 미초과 시 정상 응답을 반환한다")
        @Test
        void getChampionRotate_withinRateLimit_returnsResponse() {
            // given
            FakeChampionClientProperties properties = createDefaultProperties();
            properties.setDelayMs(0);
            properties.setRateLimitRequests(10);
            properties.setRateLimitSeconds(10);
            FakeChampionClientAdapter adapter = new FakeChampionClientAdapter(properties);

            // when & then
            for (int i = 0; i < 10; i++) {
                ChampionRotate result = adapter.getChampionRotate("kr");
                assertThat(result).isNotNull();
            }
        }

        @DisplayName("REJECT_SILENTLY 전략은 rate limit 초과해도 예외를 발생시키지 않는다")
        @Test
        void getChampionRotate_rejectSilently_noException() {
            // given
            FakeChampionClientProperties properties = createDefaultProperties();
            properties.setDelayMs(0);
            properties.setRateLimitRequests(1);
            properties.setRateLimitSeconds(10);
            properties.setRateLimitStrategy(FakeChampionClientProperties.RateLimitStrategy.REJECT_SILENTLY);
            FakeChampionClientAdapter adapter = new FakeChampionClientAdapter(properties);

            // when - 여러 요청 모두 예외 없이 처리
            adapter.getChampionRotate("kr");
            ChampionRotate result = adapter.getChampionRotate("kr");

            // then
            assertThat(result).isNotNull();
        }
    }

    private FakeChampionClientProperties createDefaultProperties() {
        FakeChampionClientProperties properties = new FakeChampionClientProperties();
        properties.setEnabled(true);
        properties.setDelayMs(200);
        properties.setRateLimitRequests(500);
        properties.setRateLimitSeconds(10);
        properties.setRateLimitStrategy(FakeChampionClientProperties.RateLimitStrategy.THROW_EXCEPTION);
        return properties;
    }
}
