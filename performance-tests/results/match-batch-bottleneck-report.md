# getMatchesBatch 병목 분석 보고서

## 테스트 환경

| 항목 | 값 |
|------|-----|
| Date | 2026-04-03 |
| Server | Spring Boot 3.3.6 / Java 17 |
| DB | PostgreSQL (외부, 125.138.61.176:41962) |
| Runtime | Docker container (eclipse-temurin:17-jre) |
| HikariCP | maximum-pool-size=20, minimum-idle=20 |
| k6 | v1.6.0 |
| Target API | `GET /api/v1/{platformId}/summoners/{puuid}/matches` |
| Test Data | master_summoners.json (10,000 puuids) |

## k6 부하테스트 결과 요약

| Rate (req/s) | avg | med | p95 | p99 | max | Error Rate | 결과 |
|---|---|---|---|---|---|---|---|
| 5 | 87ms | 82ms | 184ms | 343ms | 443ms | 0% | PASS |
| 50 | 26ms | 16ms | 99ms | 191ms | 388ms | 0% | PASS |
| 200 | 30ms | 20ms | 74ms | 234ms | 597ms | 0% | PASS |
| 300 | 55ms | 35ms | 124ms | 615ms | 2.9s | 0% | PASS |
| 400 | 824ms | 55ms | **2.48s** | **7.66s** | 27.5s | 0.4% | **FAIL** |
| 500 | 1.84s | 1.48s | **3.12s** | **8.13s** | 28.0s | 0.6% | **FAIL** |

- **안정 처리량**: ~300 req/s
- **한계점**: 400 req/s부터 p95가 124ms → 2.48s로 급등 (20배)
- **특징**: median은 55ms로 낮지만 p95/p99가 폭등하는 tail latency 패턴

## Prometheus 모니터링 지표 (부하 구간 13:27 ~ 13:28 KST)

### HTTP Request Rate
```
13:27:00     17.1 req/s
13:27:05     20.3 req/s
13:27:10     21.7 req/s
13:27:15     21.5 req/s
13:27:20     23.3 req/s  ← peak
13:27:25     22.0 req/s
13:27:30     20.7 req/s
13:27:35     20.4 req/s
13:27:40     19.6 req/s
13:27:45     19.3 req/s
13:27:50     13.4 req/s
13:27:55     10.7 req/s
```

### HikariCP Active Connections
```
13:27:00  Active= 6/20
13:27:05  Active= 4/20
13:27:10  Active= 5/20
13:27:15  Active= 7/20  ← max
13:27:20  Active= 7/20  ← max
13:27:25  Active= 5/20
13:27:30  Active= 6/20
13:27:35  Active= 6/20
13:27:40  Active= 5/20
13:27:45  Active= 5/20
```

### HikariCP Pending / Timeout
```
Pending:  항상 0 (커넥션 대기 없음)
Timeout:  0 (커넥션 획득 실패 없음)
```

### HikariCP Acquire Time
```
부하 중:  1~2ms (정상)
```

### JVM Threads
```
Live threads: 172 → 247 (부하 시 +75)
```

## 병목 원인 분석

### 결론: ForkJoinPool.commonPool() 스레드 경합

`MatchPersistenceAdapter.getMatchesBatch()` (line 105~183)에서 4개의 `CompletableFuture.supplyAsync()`가 `ForkJoinPool.commonPool()`을 사용한다.

```java
CompletableFuture.supplyAsync(() -> matchRepositoryCustom.getMatchSummoners(matchIds))  // task 1
CompletableFuture.supplyAsync(() -> matchRepositoryCustom.getMatchTeams(matchIds))       // task 2
CompletableFuture.supplyAsync(() -> timelineRepositoryCustom.selectItemEvents(matchIds)) // task 3
CompletableFuture.supplyAsync(() -> timelineRepositoryCustom.selectSkillEvents(matchIds))// task 4
```

`ForkJoinPool.commonPool()` 크기 = `availableProcessors() - 1` (Docker 컨테이너 기준 약 3~7)

### 근거

| 지표 | 관찰값 | 의미 |
|---|---|---|
| HikariCP Active | max **7**/20 | 커넥션 풀 여유 13개. 풀이 병목이 **아님** |
| HikariCP Pending | **0** | DB 커넥션 대기 스레드 없음 |
| HikariCP Timeout | **0** | 커넥션 획득 실패 없음 |
| JVM Live Threads | 172→**247** | Tomcat 스레드는 충분히 증가 |
| Response Time | **30초+** | ForkJoinPool 큐에서 대기하는 시간 |

### 메커니즘

1. 요청이 들어오면 Tomcat 스레드가 처리 시작
2. `getMatchesBatch`에서 4개 async task를 commonPool에 제출
3. commonPool 스레드 ~7개가 모든 요청의 async task를 처리해야 함
4. 동시 요청 20개 × 4 task = 80개 task가 7개 스레드를 경합
5. 대부분의 task는 큐에서 **수십 초간 대기**
6. Active 커넥션이 7에서 멈추는 이유: commonPool 스레드 수만큼만 동시에 DB 접근 가능

## 개선 방향 (검토 필요)

1. **CompletableFuture 제거, 순차 실행으로 변경**: 병렬 실행의 이점이 commonPool 경합으로 상쇄되므로, 단순 순차 실행이 더 나을 수 있음
2. **전용 TaskExecutor 사용**: I/O bound 작업에 맞는 별도 스레드 풀 주입
3. **쿼리 통합**: 4개 배치 쿼리를 1~2개로 줄여 병렬 필요성 자체를 제거
