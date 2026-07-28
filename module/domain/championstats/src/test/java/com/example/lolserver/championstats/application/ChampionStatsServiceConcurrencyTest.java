package com.example.lolserver.championstats.application;

import com.example.lolserver.shared.TierFilter;
import com.example.lolserver.championstats.application.model.readmodel.ChampionStatsReadModel;
import com.example.lolserver.championstats.application.model.readmodel.PositionChampionStatsReadModel;
import com.example.lolserver.championstats.application.port.out.ChampionStatsCachePort;
import com.example.lolserver.championstats.application.port.out.ChampionStatsMetricsPort;
import com.example.lolserver.championstats.application.port.out.ChampionStatsQueryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChampionStatsServiceConcurrencyTest {

    @Mock
    private ChampionStatsMetricsPort championStatsMetricsPort;

    private CountingQueryPort queryPort;
    private InMemoryCachePort cachePort;
    private ChampionStatsService service;
    private ExecutorService callerExecutor;

    @BeforeEach
    void setUp() {
        queryPort = new CountingQueryPort();
        cachePort = new InMemoryCachePort();
        service = new ChampionStatsService(
                queryPort, cachePort, championStatsMetricsPort,
                Runnable::run, true);
        callerExecutor = Executors.newFixedThreadPool(16);
    }

    @AfterEach
    void tearDown() {
        callerExecutor.shutdownNow();
    }

    @DisplayName("동시에 cache miss 로 진입한 N 요청은 BQ 를 단 1회만 호출한다 (single-flight)")
    @Test
    void concurrentCacheMiss_invokesBigQueryOnce() throws InterruptedException {
        // given
        int callers = 10;
        int championId = 13;
        String patch = "16.1";
        String platformId = "KR";
        TierFilter tierFilter = TierFilter.of("EMERALD");

        // by-position 캐시 hit 으로 nested single-flight 단락
        cachePort.seedByPosition(patch, platformId, "EMERALD", List.of());

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(callers);

        // when
        for (int i = 0; i < callers; i++) {
            callerExecutor.submit(() -> {
                try {
                    startGate.await();
                    service.getChampionStats(championId, patch, platformId, tierFilter);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishGate.countDown();
                }
            });
        }
        startGate.countDown();
        boolean done = finishGate.await(10, TimeUnit.SECONDS);

        // then
        assertThat(done).isTrue();
        assertThat(queryPort.winRatesCalls.get())
                .as("동시 진입 N 요청에 대해 getChampionWinRates 는 단일 호출자만 실행해야 한다")
                .isEqualTo(1);
    }

    private static class CountingQueryPort implements ChampionStatsQueryPort {
        final AtomicInteger winRatesCalls = new AtomicInteger();

        @Override
        public List<com.example.lolserver.championstats.application.model.readmodel.ChampionWinRateReadModel>
                getChampionWinRates(int championId, String patch, String platformId, TierFilter tierFilter) {
            winRatesCalls.incrementAndGet();
            // 모방: BQ 호출이 잠시 걸린다고 가정 — 다른 caller 들이 폴링/락 대기에 진입하도록
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }

        @Override
        public List<com.example.lolserver.championstats.application.model.readmodel.ChampionAverageStatsReadModel>
                getChampionAverageStats(int championId, String patch, String platformId, TierFilter tierFilter) {
            return List.of();
        }

        @Override
        public List<com.example.lolserver.championstats.application.model.readmodel.ChampionMatchupReadModel>
                getChampionMatchups(int championId, String patch, String platformId, TierFilter tierFilter, String position) {
            return List.of();
        }

        @Override
        public List<com.example.lolserver.championstats.application.model.readmodel.ChampionRuneBuildReadModel>
                getChampionRuneBuilds(int championId, String patch, String platformId, TierFilter tierFilter, String position) {
            return List.of();
        }

        @Override
        public List<com.example.lolserver.championstats.application.model.readmodel.ChampionSpellStatsReadModel>
                getChampionSpellStats(int championId, String patch, String platformId, TierFilter tierFilter, String position) {
            return List.of();
        }

        @Override
        public List<com.example.lolserver.championstats.application.model.readmodel.ChampionSkillBuildReadModel>
                getChampionSkillBuilds(int championId, String patch, String platformId, TierFilter tierFilter, String position) {
            return List.of();
        }

        @Override
        public List<com.example.lolserver.championstats.application.model.readmodel.ChampionStartItemBuildReadModel>
                getChampionStartItemBuilds(int championId, String patch, String platformId, TierFilter tierFilter, String position) {
            return List.of();
        }

        @Override
        public List<com.example.lolserver.championstats.application.model.readmodel.ChampionBootBuildReadModel>
                getChampionBootBuilds(int championId, String patch, String platformId, TierFilter tierFilter, String position) {
            return List.of();
        }

        @Override
        public List<com.example.lolserver.championstats.application.model.readmodel.ChampionItemBuildReadModel>
                getChampionItemBuilds(int championId, String patch, String platformId, TierFilter tierFilter, String position) {
            return List.of();
        }

        @Override
        public Map<String, List<com.example.lolserver.championstats.application.model.readmodel.ChampionRateReadModel>>
                getChampionStatsByPosition(String patch, String platformId, TierFilter tierFilter) {
            return Map.of();
        }
    }

    private static class InMemoryCachePort implements ChampionStatsCachePort {
        private final ConcurrentHashMap<String, ChampionStatsReadModel> detailCache = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<PositionChampionStatsReadModel>> positionsCache = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

        void seedByPosition(String patch, String platformId, String tierDisplay,
                            List<PositionChampionStatsReadModel> value) {
            positionsCache.put(positionsKey(patch, platformId, tierDisplay), value);
        }

        @Override
        public ChampionStatsReadModel findChampionStats(
                int championId, String patch, String platformId, String tierDisplay) {
            return detailCache.get(detailKey(championId, patch, platformId, tierDisplay));
        }

        @Override
        public void saveChampionStats(int championId, String patch, String platformId,
                                       String tierDisplay, ChampionStatsReadModel stats) {
            detailCache.put(detailKey(championId, patch, platformId, tierDisplay), stats);
        }

        @Override
        public List<PositionChampionStatsReadModel> findChampionStatsByPosition(
                String patch, String platformId, String tierDisplay) {
            return positionsCache.get(positionsKey(patch, platformId, tierDisplay));
        }

        @Override
        public void saveChampionStatsByPosition(String patch, String platformId, String tierDisplay,
                                                 List<PositionChampionStatsReadModel> stats) {
            positionsCache.put(positionsKey(patch, platformId, tierDisplay), stats);
        }

        @Override
        public boolean tryLockDetail(int championId, String patch, String platformId, String tierDisplay) {
            return tryLock("detail:" + detailKey(championId, patch, platformId, tierDisplay));
        }

        @Override
        public void unlockDetail(int championId, String patch, String platformId, String tierDisplay) {
            unlock("detail:" + detailKey(championId, patch, platformId, tierDisplay));
        }

        @Override
        public boolean tryLockByPosition(String patch, String platformId, String tierDisplay) {
            return tryLock("positions:" + positionsKey(patch, platformId, tierDisplay));
        }

        @Override
        public void unlockByPosition(String patch, String platformId, String tierDisplay) {
            unlock("positions:" + positionsKey(patch, platformId, tierDisplay));
        }

        private boolean tryLock(String key) {
            ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
            try {
                return lock.tryLock(3L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        private void unlock(String key) {
            ReentrantLock lock = locks.get(key);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        private String detailKey(int championId, String patch, String platformId, String tierDisplay) {
            return platformId + ":" + championId + ":" + patch + ":" + tierDisplay;
        }

        private String positionsKey(String patch, String platformId, String tierDisplay) {
            return platformId + ":" + patch + ":" + tierDisplay;
        }
    }
}
