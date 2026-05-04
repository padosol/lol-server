# infra:persistence:redis

Redis 어댑터 (driven adapter). 캐시, 세션 (Refresh Token, OAuth State), 분산 락, Bucket4j 레이트 리밋, 갱신 마커 등 Postgres 와 별도로 휘발/단기 상태를 저장한다. `StringRedisTemplate` + Redisson + Lettuce `RedisClient` 모두 사용.

## Boundaries

- 허용: `core:lol-server-domain`, `core:enum`, `spring-boot-starter-data-redis`, `redisson-spring-boot-starter`
- 금지: JPA, RestClient
- 도메인 out port (`RefreshTokenPort`, `OAuthStatePort`, `SummonerCachePort`, `ChampionCachePort`, `VersionCachePort`, `SpectatorCachePort`, `ChampionStatsCachePort`) 만 구현 — 자체 인터페이스 새로 만들지 말 것

## Layout

- `repository/<domain>/Xxx*Adapter.java` — 도메인 out port 구현 (`@Component`)
- `service/SummonerCacheAdapter.java` — Summoner 캐시/락/쿨다운 통합 어댑터 (분산 락은 여기 reference)
- `model/SummonerRenewalSession.java`, `SummonerRankSession.java` — Redis 직렬화 대상 record/POJO
- `config/RedisConfig.java` — `StringRedisTemplate` 빈
- `config/RedissonConfig.java` — `RedissonClient` 빈 (분산 락용)
- `config/RateLimiterConfig.java` — Lettuce `RedisClient` 빈 (Bucket4j 레이트 리밋용)

## Key Files

- `repository/member/RefreshTokenRedisAdapter.java` — 가장 단순한 어댑터 reference (KEY_PREFIX 상수 + TTL 사용)
- `repository/member/OAuthStateRedisAdapter.java` — OAuth state CSRF 방어용 단기 토큰 저장
- `service/SummonerCacheAdapter.java` — `RLock` 분산 락, `ScanOptions` 키 스캔, 다중 prefix 통합 어댑터 reference
- `config/RedissonConfig.java` — Redisson 클라이언트 (분산 락 필요한 어댑터에서 주입)
- `config/RateLimiterConfig.java` — Bucket4j 가 사용할 Lettuce `RedisClient`

## Common Modifications

- **새 캐시 추가**:
  1. 도메인에 `XxxCachePort` out port 정의
  2. `repository/<domain>/Xxx*Adapter.java implements XxxCachePort` 작성, `KEY_PREFIX` 상수 + TTL 명시
  3. 직렬화는 단순 문자열이면 `StringRedisTemplate`, 객체면 `RedisTemplate<String, Xxx>` 와 Jackson 직렬화기를 따로 빈으로 등록
- **분산 락 추가**: `RedissonClient.getLock("<prefix>:" + key).tryLock(wait, lease, SECONDS)` 패턴, finally 가 아닌 `unlock` 메서드 분리 후 `isHeldByCurrentThread()` 가드
- **TTL 정책 변경**: 어댑터 상수 (`LOCK_LEASE_TIME` 등) 만 수정. 도메인은 TTL 모름

## Failure Patterns / Gotchas

- ❌ `RedisTemplate<Object, Object>` 자동 직렬화에 의존 — JdkSerializationRedisSerializer 가 패키지 변경 시 데이터 깨짐
  ✅ `StringRedisTemplate` + 명시적 JSON 직렬화 (record / POJO 는 Jackson 으로 string 화)
- ❌ `lock.unlock()` 무조건 호출 — 다른 스레드가 잡은 락 해제 시 `IllegalMonitorStateException`
  ✅ `if (lock.isHeldByCurrentThread()) lock.unlock();` (`SummonerCacheAdapter` 패턴)
- ❌ `KEYS xxx:*` 사용 — 운영 Redis blocking
  ✅ `ScanOptions` + `Cursor` (`SummonerCacheAdapter.getRefreshingPuuids` 참고)
- ❌ TTL 없이 `set(key, value)` — 무한히 쌓임
  ✅ 모든 set 은 TTL 명시 (`set(key, val, ttl, TimeUnit.X)`)
- ❌ Adapter 가 도메인이 모르는 캐시 hit/miss 시그널을 노출 (`Optional<RedisCacheEntry>`)
  ✅ 도메인 타입 (`Optional<String>`, `Optional<XxxReadModel>`) 만 반환

## Cross-Module Dependencies

- depends on: `core:lol-server-domain` (out port), `core:enum`
- consumed by: `app:application` (런타임 빈 주입)
- `infra:api` 는 직접 의존 안 하지만, JWT 인증 플로우가 `RefreshTokenPort` (이 모듈 구현체) 에 의존 — 빈이 빠지면 인증 깨짐

## See Also

- [core:lol-server-domain](../../core/lol-server-domain/CLAUDE.md) — 캐시 port 의 출처
- [postgresql](../postgresql/CLAUDE.md) — 같은 도메인의 영속 어댑터 (캐시 vs DB 책임 구분)
- [client/oauth](../client/oauth/CLAUDE.md) — OAuth state 저장 (`OAuthStatePort`) 의 사용처
