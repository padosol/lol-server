# VT(Virtual Thread)에 traceId 전파 + 8자리 traceId

## Context

MDCFilter에서 요청마다 traceId를 MDC에 설정하지만, CompletableFuture.supplyAsync로 Virtual Thread에서 실행되는 코드는 부모 스레드의 MDC를 상속받지 못해 로그에 traceId가 빈 값으로 출력된다.
또한 현재 UUID 전체(36자)를 사용하고 있어 8자리로 줄여야 한다.

## 영향받는 VT 실행 지점

1. `MatchPersistenceAdapter.getMatchesBatch()` — `queryExecutor` (VT) 사용
2. `RiotAccountResolver.lookupAllStats()` — 기본 ForkJoinPool 사용 (VT는 아니지만 MDC 전파 안 됨)

## 변경 파일 및 내용

### 1. `MDCFilter.java` — traceId 8자리로 변경

**경로:** `module/infra/api/src/main/java/com/example/lolserver/controller/filter/MDCFilter.java`

UUID의 앞 8자리만 사용:
```java
String traceId = UUID.randomUUID().toString().substring(0, 8);
MDC.put("traceId", traceId);
```

### 2. `AsyncQueryConfig.java` — MDC 전파 Executor 래핑

**경로:** `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/config/AsyncQueryConfig.java`

VT executor를 MDC 전파 래퍼로 감싼다:
```java
@Bean("queryExecutor")
public Executor queryExecutor() {
    Executor vtExecutor = Executors.newVirtualThreadPerTaskExecutor();
    return mdcDelegatingExecutor(vtExecutor);
}

private Executor mdcDelegatingExecutor(Executor delegate) {
    return runnable -> {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        delegate.execute(() -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        });
    };
}
```

### 3. `RiotAccountResolver.java` — MDC 전파 적용

**경로:** `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/duo/application/RiotAccountResolver.java`

`CompletableFuture.supplyAsync`에 executor가 지정되지 않아 기본 ForkJoinPool을 사용 중이다.
MDC context를 캡처해서 람다 내부에서 복원한다:

```java
public RiotAccountStats lookupAllStats(String puuid) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();

    CompletableFuture<TierInfo> tierFuture =
            CompletableFuture.supplyAsync(() -> withMdc(contextMap, () -> lookupTierInfo(puuid)));
    CompletableFuture<List<MostChampion>> championsFuture =
            CompletableFuture.supplyAsync(() -> withMdc(contextMap, () -> lookupMostChampions(puuid)));
    CompletableFuture<RecentGameSummary> recentGameFuture =
            CompletableFuture.supplyAsync(() -> withMdc(contextMap, () -> lookupRecentGameSummary(puuid)));
    ...
}

private <T> T withMdc(Map<String, String> contextMap, Supplier<T> supplier) {
    if (contextMap != null) {
        MDC.setContextMap(contextMap);
    }
    try {
        return supplier.get();
    } finally {
        MDC.clear();
    }
}
```

단, `RiotAccountResolver`는 `core:lol-server-domain` 모듈에 있으므로 SLF4J MDC 의존성이 있는지 확인 필요. SLF4J는 로깅 API이므로 도메인 모듈에서 사용 가능 (Lombok @Slf4j도 이미 사용 패턴이 있음).

## 검증 방법

1. `./gradlew :module:infra:persistence:postgresql:compileJava` — 컴파일 확인
2. `./gradlew :module:core:lol-server-domain:compileJava` — 컴파일 확인
3. `./gradlew test` — 전체 테스트 통과
4. 로컬 실행 후 로그에서 VT 실행 메서드의 traceId가 8자리로 출력되는지 확인
