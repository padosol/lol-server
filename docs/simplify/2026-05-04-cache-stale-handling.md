# Simplify Review: cache-stale-handling

**Date:** 2026-05-04
**Target:** `ChampionStatsCacheAdapter` — Redis SerializationException(stale shape) 핸들링 + 로그 레벨 조정

---

## 1. Code Reuse Review

### Findings

| # | Item | Action |
|---|------|--------|
| 1 | 다른 Redis 어댑터(`RefreshTokenRedisAdapter`, `OAuthStateRedisAdapter`, `SpectatorRedisAdapter`, `VersionRedisAdapter`, `ChampionCacheAdapter`)에 stale-shape 처리 + quiet eviction 패턴 부재. 공유 `RedisTemplateHelper.getQuietly(key)` 추출 가능 | **Skip** — 현재 단일 어댑터에서만 필요. CLAUDE.md "Don't add abstractions beyond what the task requires". 두 번째 어댑터에서 동일 요구가 생기면 그때 추출 |
| 2 | "log without stack trace" 공유 헬퍼 부재 | **Skip** — SLF4J `log.debug(msg, e.getMessage())` 자체로 충분, 별도 헬퍼 불필요 |

---

## 2. Code Quality Review

### Findings

| # | Item | Severity | Action |
|---|------|----------|--------|
| 1 | 두 `find*` 메서드의 try/catch 구조 ~10줄 중복 | Medium | **Fixed** — `tryGetFromCache(String key)` 제네릭 헬퍼로 추출. 두 caller가 키 빌드 1줄로 축소 |
| 2 | `evictQuietly`의 `catch (Exception ignored)` 광범위 catch | High | **Skip** — "quietly" 시맨틱상 광범위 catch 의도. `OOM`/`Error`는 `Exception` 미상속이라 통과. `RedisSystemException`만 잡으면 alert 받지 못한 unknown exception이 propagate되어 read 실패 → null 반환 흐름이 깨짐 |
| 3 | 일반 `Exception` → DEBUG로 강등 시 실제 Redis 장애 silent 됨. WARN으로 승격 권장 | Medium | **Skip** — **사용자가 명시적으로 "에러 나오지 않도록" 요청**. Health check / Redis 모니터링은 인프라 레벨에서 별도 수행 가정 |
| 4 | Korean 로그 메시지 → 상수 추출 (CLAUDE.md "매직 스트링 금지" 위반) | Medium | **Skip** — False positive. CLAUDE.md 매직 스트링 규칙은 enum-backed 도메인 값 (예: `OAuthProvider.RIOT.name()`) 대상. 단순 로그 문자열은 해당 없음 |
| 5 | `e.getMessage()`가 null일 가능성 | Low | **Skip** — SLF4J가 null을 "null" 문자열로 안전하게 처리, 추가 분기 불필요 |

---

## 3. Efficiency Review

### Findings

| # | Item | Impact | Action |
|---|------|--------|--------|
| 1 | 키 빌드를 try 밖으로 이동한 비용 | Negligible | **Skip** — 문자열 concat 마이크로초 단위 |
| 2 | SerializationException 시 동기 DEL이 요청 latency에 추가 | Medium | **Skip** — stale shape는 Redis prefix bump (수개월 1회) 시점에만 발생하는 드문 이벤트. 비동기 큐 도입 비용 > 효과 |
| 3 | 동시 요청 SerializationException 스톰 시 중복 DEL 발생 | Medium | **Skip** — DEL은 idempotent. 6시간 TTL 동안만 발생하는 transient 부하. Caffeine dedup 캐시 도입은 과도한 최적화 |
| 4 | 로그 레벨 변경 비용 | Negligible | **Skip** |
| 5 | `evictQuietly`의 광범위 catch | Negligible | **Skip** — `RedisTemplate.delete()`는 리소스 보유 없음 |

---

## Summary

| File | Change |
|------|--------|
| `module/infra/persistence/redis/.../ChampionStatsCacheAdapter.java` | 두 `find*` 메서드의 try/catch 중복을 `tryGetFromCache(String key)` 제네릭 헬퍼로 통합. SerializationException 정책 변경 시 단일 지점만 수정 |

**Build result:** BUILD SUCCESSFUL (`./gradlew :module:infra:persistence:redis:test`)

**Findings 통계:** 12건 중 1건 Fixed, 11건 Skip
