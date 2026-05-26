package com.example.lolserver.championstats.application;

import com.example.lolserver.TierFilter;
import com.example.lolserver.championstats.application.model.ChampionAverageStatsReadModel;
import com.example.lolserver.championstats.application.model.ChampionBootBuildReadModel;
import com.example.lolserver.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.championstats.application.model.ChampionPositionStatsReadModel;
import com.example.lolserver.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.championstats.application.model.ChampionSpellStatsReadModel;
import com.example.lolserver.championstats.application.model.ChampionStartItemBuildReadModel;
import com.example.lolserver.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.championstats.application.model.ChampionRateReadModel;
import com.example.lolserver.championstats.application.model.ChampionWinRateReadModel;
import com.example.lolserver.championstats.application.model.PositionChampionStatsReadModel;
import com.example.lolserver.championstats.application.port.in.ChampionStatsQueryUseCase;
import com.example.lolserver.championstats.application.port.out.ChampionStatsCachePort;
import com.example.lolserver.championstats.application.port.out.ChampionStatsMetricsPort;
import com.example.lolserver.championstats.application.port.out.ChampionStatsQueryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ChampionStatsService implements ChampionStatsQueryUseCase {

    private static final int CACHE_POLL_ATTEMPTS = 15;
    private static final long CACHE_POLL_INTERVAL_MS = 200L;

    private final ChampionStatsQueryPort championStatsQueryPort;
    private final ChampionStatsCachePort championStatsCachePort;
    private final ChampionStatsMetricsPort championStatsMetricsPort;
    private final Executor queryExecutor;
    private final boolean cacheEnabled;

    public ChampionStatsService(
            ChampionStatsQueryPort championStatsQueryPort,
            ChampionStatsCachePort championStatsCachePort,
            ChampionStatsMetricsPort championStatsMetricsPort,
            @Qualifier("queryExecutor") Executor queryExecutor,
            @Value("${champion-stats.cache.enabled:true}") boolean cacheEnabled) {
        this.championStatsQueryPort = championStatsQueryPort;
        this.championStatsCachePort = championStatsCachePort;
        this.championStatsMetricsPort = championStatsMetricsPort;
        this.queryExecutor = queryExecutor;
        this.cacheEnabled = cacheEnabled;
    }

    public ChampionStatsReadModel getChampionStats(
            int championId, String patch, String platformId, TierFilter tierFilter) {

        String tierDisplay = tierFilter.toDisplayString();

        if (!cacheEnabled) {
            return computeChampionStats(championId, patch, platformId, tierFilter, tierDisplay);
        }

        return loadWithSingleFlight(
                () -> championStatsCachePort.findChampionStats(championId, patch, platformId, tierDisplay),
                () -> championStatsCachePort.tryLockDetail(championId, patch, platformId, tierDisplay),
                () -> championStatsCachePort.unlockDetail(championId, patch, platformId, tierDisplay),
                () -> computeChampionStats(championId, patch, platformId, tierFilter, tierDisplay),
                value -> championStatsCachePort.saveChampionStats(championId, patch, platformId, tierDisplay, value),
                ChampionStatsMetricsPort.ENDPOINT_DETAIL
        );
    }

    public List<PositionChampionStatsReadModel> getChampionStatsByPosition(
            String patch, String platformId, TierFilter tierFilter) {

        String tierDisplay = tierFilter.toDisplayString();

        if (!cacheEnabled) {
            return computeChampionStatsByPosition(patch, platformId, tierFilter);
        }

        return loadWithSingleFlight(
                () -> championStatsCachePort.findChampionStatsByPosition(patch, platformId, tierDisplay),
                () -> championStatsCachePort.tryLockByPosition(patch, platformId, tierDisplay),
                () -> championStatsCachePort.unlockByPosition(patch, platformId, tierDisplay),
                () -> computeChampionStatsByPosition(patch, platformId, tierFilter),
                value -> championStatsCachePort.saveChampionStatsByPosition(patch, platformId, tierDisplay, value),
                ChampionStatsMetricsPort.ENDPOINT_POSITIONS
        );
    }

    private ChampionStatsReadModel computeChampionStats(
            int championId, String patch, String platformId, TierFilter tierFilter, String tierDisplay) {

        CompletableFuture<List<ChampionWinRateReadModel>> winRatesFuture =
                CompletableFuture.supplyAsync(() ->
                        championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tierFilter),
                        queryExecutor);
        CompletableFuture<Map<String, ChampionRateReadModel>> ratesFuture =
                CompletableFuture.supplyAsync(() ->
                        lookupChampionRateByPosition(championId, patch, platformId, tierFilter),
                        queryExecutor);
        CompletableFuture<Map<String, ChampionAverageStatsReadModel>> averagesFuture =
                CompletableFuture.supplyAsync(() ->
                        championStatsQueryPort.getChampionAverageStats(championId, patch, platformId, tierFilter)
                                .stream()
                                .collect(Collectors.toMap(ChampionAverageStatsReadModel::teamPosition, a -> a)),
                        queryExecutor);

        List<ChampionWinRateReadModel> winRates = winRatesFuture.join();

        List<CompletableFuture<ChampionPositionStatsReadModel>> positionFutures = winRates.stream()
                .map(wr -> buildPositionStatsAsync(
                        championId, patch, platformId, tierFilter, wr, ratesFuture, averagesFuture))
                .toList();

        List<ChampionPositionStatsReadModel> positions = positionFutures.stream()
                .map(CompletableFuture::join)
                .toList();

        return new ChampionStatsReadModel(tierDisplay, positions);
    }

    private CompletableFuture<ChampionPositionStatsReadModel> buildPositionStatsAsync(
            int championId, String patch, String platformId, TierFilter tierFilter,
            ChampionWinRateReadModel winRate,
            CompletableFuture<Map<String, ChampionRateReadModel>> ratesFuture,
            CompletableFuture<Map<String, ChampionAverageStatsReadModel>> averagesFuture) {

        String position = winRate.teamPosition();
        long totalSamples = winRate.totalGames();

        CompletableFuture<List<ChampionMatchupReadModel>> matchupsF = async(() ->
                championStatsQueryPort.getChampionMatchups(championId, patch, platformId, tierFilter, position));
        CompletableFuture<List<ChampionRuneBuildReadModel>> runeBuildsF = async(() ->
                withConfidence(championStatsQueryPort.getChampionRuneBuilds(
                        championId, patch, platformId, tierFilter, position),
                        totalSamples, ChampionRuneBuildReadModel::games,
                        ChampionRuneBuildReadModel::winRate, ChampionRuneBuildReadModel::withConfidence));
        CompletableFuture<List<ChampionSpellStatsReadModel>> spellStatsF = async(() ->
                withConfidence(championStatsQueryPort.getChampionSpellStats(
                        championId, patch, platformId, tierFilter, position),
                        totalSamples, ChampionSpellStatsReadModel::games,
                        ChampionSpellStatsReadModel::winRate, ChampionSpellStatsReadModel::withConfidence));
        CompletableFuture<List<ChampionSkillBuildReadModel>> skillBuildsF = async(() ->
                withConfidence(championStatsQueryPort.getChampionSkillBuilds(
                                championId, patch, platformId, tierFilter, position).stream()
                                .filter(b -> b.skillBuild() != null && !b.skillBuild().isBlank())
                                .toList(),
                        totalSamples, ChampionSkillBuildReadModel::games,
                        ChampionSkillBuildReadModel::winRate, ChampionSkillBuildReadModel::withConfidence));
        CompletableFuture<List<ChampionStartItemBuildReadModel>> startItemBuildsF = async(() ->
                withConfidence(championStatsQueryPort.getChampionStartItemBuilds(
                        championId, patch, platformId, tierFilter, position),
                        totalSamples, ChampionStartItemBuildReadModel::games,
                        ChampionStartItemBuildReadModel::winRate, ChampionStartItemBuildReadModel::withConfidence));
        CompletableFuture<List<ChampionBootBuildReadModel>> bootBuildsF = async(() ->
                withConfidence(championStatsQueryPort.getChampionBootBuilds(
                        championId, patch, platformId, tierFilter, position),
                        totalSamples, ChampionBootBuildReadModel::games,
                        ChampionBootBuildReadModel::winRate, ChampionBootBuildReadModel::withConfidence));
        CompletableFuture<List<ChampionItemBuildReadModel>> itemBuildsF = async(() ->
                withConfidence(championStatsQueryPort.getChampionItemBuilds(
                        championId, patch, platformId, tierFilter, position),
                        totalSamples, ChampionItemBuildReadModel::games,
                        ChampionItemBuildReadModel::winRate, ChampionItemBuildReadModel::withConfidence));

        return CompletableFuture.allOf(matchupsF, runeBuildsF, spellStatsF, skillBuildsF,
                        startItemBuildsF, bootBuildsF, itemBuildsF, ratesFuture, averagesFuture)
                .thenApply(__ -> {
                    ChampionRateReadModel rate = ratesFuture.join().get(position);
                    return new ChampionPositionStatsReadModel(
                            position, winRate.totalWinRate(),
                            rate != null ? rate.pickRate() : 0.0,
                            rate != null ? rate.banRate() : 0.0,
                            rate != null ? rate.tier() : null,
                            winRate.totalGames(),
                            averagesFuture.join().get(position),
                            matchupsF.join(), runeBuildsF.join(), spellStatsF.join(), skillBuildsF.join(),
                            startItemBuildsF.join(), bootBuildsF.join(), itemBuildsF.join());
                });
    }

    private <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, queryExecutor);
    }

    private static <T> List<T> withConfidence(
            List<T> source, long totalSamples,
            java.util.function.ToLongFunction<T> gamesGetter,
            java.util.function.ToDoubleFunction<T> winRateGetter,
            ConfidenceApplier<T> applier) {
        return source.stream()
                .map(b -> applier.apply(b, totalSamples,
                        wilson(gamesGetter.applyAsLong(b), winRateGetter.applyAsDouble(b))))
                .toList();
    }

    @FunctionalInterface
    private interface ConfidenceApplier<T> {
        T apply(T source, long totalSamples, double confidence);
    }

    private List<PositionChampionStatsReadModel> computeChampionStatsByPosition(
            String patch, String platformId, TierFilter tierFilter) {

        Map<String, List<ChampionRateReadModel>> groupedByPosition =
                championStatsQueryPort.getChampionStatsByPosition(patch, platformId, tierFilter);

        return groupedByPosition.entrySet().stream()
                .map(entry -> new PositionChampionStatsReadModel(
                        entry.getKey(),
                        assignTiersIfMissing(entry.getValue())))
                .toList();
    }

    private Map<String, ChampionRateReadModel> lookupChampionRateByPosition(
            int championId, String patch, String platformId, TierFilter tierFilter) {
        List<PositionChampionStatsReadModel> byPosition =
                getChampionStatsByPosition(patch, platformId, tierFilter);

        Map<String, ChampionRateReadModel> result = new HashMap<>();
        for (PositionChampionStatsReadModel position : byPosition) {
            for (ChampionRateReadModel rate : position.champions()) {
                if (rate.championId() == championId) {
                    result.put(position.teamPosition(), rate);
                    break;
                }
            }
        }
        return result;
    }

    private <T> T loadWithSingleFlight(
            Supplier<T> cacheLookup,
            BooleanSupplier tryAcquireLock,
            Runnable releaseLock,
            Supplier<T> computeFresh,
            Consumer<T> saveCache,
            String endpoint) {

        T cached = cacheLookup.get();
        if (cached != null) {
            return cached;
        }

        boolean acquired = tryAcquireLock.getAsBoolean();
        if (acquired) {
            try {
                T doubleCheck = cacheLookup.get();
                if (doubleCheck != null) {
                    return doubleCheck;
                }
                T fresh = computeFresh.get();
                saveCache.accept(fresh);
                return fresh;
            } finally {
                releaseLock.run();
            }
        }

        for (int attempt = 0; attempt < CACHE_POLL_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(CACHE_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            cached = cacheLookup.get();
            if (cached != null) {
                return cached;
            }
        }

        log.warn("Single-flight fallback - endpoint: {}", endpoint);
        championStatsMetricsPort.recordSingleFlightFallback(endpoint);
        T fresh = computeFresh.get();
        saveCache.accept(fresh);
        return fresh;
    }

    private static double wilson(long games, double winRate) {
        long wins = Math.round(winRate * games);
        return WilsonScore.lowerBound95(wins, games);
    }

    private static List<ChampionRateReadModel> assignTiersIfMissing(List<ChampionRateReadModel> champions) {
        if (!champions.isEmpty() && champions.get(0).tier() != null) {
            return champions;
        }
        return ChampionTierCalculator.assignTiers(champions);
    }
}
