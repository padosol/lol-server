# Champion Stats 캐시 성능 비교 보고서

## 테스트 환경

| 항목 | 값 |
|------|-----|
| Date | YYYY-MM-DD |
| Server | Spring Boot 3.3.6 / Java 17 |
| ClickHouse | (버전 기입) |
| Redis | (버전 기입) |
| k6 Rate | 50 req/s |
| Duration | 60s |
| VUs | 50 (pre-allocated), 100 (max) |
| Patch | 16.1 |
| Tiers | EMERALD, DIAMOND, MASTER, GRANDMASTER, CHALLENGER |

## Cache OFF (champion-stats.cache.enabled=false)

### Detail API 메트릭

| Metric | Value |
|--------|-------|
| avg | {ms} |
| p50 | {ms} |
| p95 | {ms} |
| p99 | {ms} |
| max | {ms} |
| Error Rate | {%} |
| Throughput | {req/s} |

### Positions API 메트릭

| Metric | Value |
|--------|-------|
| avg | {ms} |
| p50 | {ms} |
| p95 | {ms} |
| p99 | {ms} |
| max | {ms} |
| Error Rate | {%} |
| Throughput | {req/s} |

### ClickHouse 커넥션

| Metric | Value |
|--------|-------|
| Active Connections (peak) | {n} |
| Timeout Count | {n} |

## Cache ON (champion-stats.cache.enabled=true)

### Detail API 메트릭

| Metric | Value |
|--------|-------|
| avg | {ms} |
| p50 | {ms} |
| p95 | {ms} |
| p99 | {ms} |
| max | {ms} |
| Error Rate | {%} |
| Throughput | {req/s} |

### Positions API 메트릭

| Metric | Value |
|--------|-------|
| avg | {ms} |
| p50 | {ms} |
| p95 | {ms} |
| p99 | {ms} |
| max | {ms} |
| Error Rate | {%} |
| Throughput | {req/s} |

### Redis 관찰

| Metric | Value |
|--------|-------|
| Cache Hit Rate | {%} |
| Redis Memory Usage | {MB} |

## 비교 요약

| Metric | Cache OFF | Cache ON | 개선율 |
|--------|-----------|----------|--------|
| Detail API p95 | {ms} | {ms} | {x}x |
| Detail API avg | {ms} | {ms} | {x}x |
| Positions API p95 | {ms} | {ms} | {x}x |
| Positions API avg | {ms} | {ms} | {x}x |
| CH Active Conn (peak) | {n} | {n} | -{n}% |
| Error Rate | {%} | {%} | ... |

## 결론

(테스트 결과를 바탕으로 캐시 도입 효과 분석)

## 실행 방법

```bash
# 캐시 OFF 테스트 (서버: champion-stats.cache.enabled=false)
k6 run --tag phase=cache-off --out json=results/champion-stats-cache-off.json scenarios/champion-stats-test.js

# 캐시 ON 테스트 (서버: champion-stats.cache.enabled=true)
k6 run --tag phase=cache-on --out json=results/champion-stats-cache-on.json scenarios/champion-stats-test.js
```
