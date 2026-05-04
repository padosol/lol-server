# infra:client:lol-repository

Riot API 호출 어댑터 (driven adapter). 도메인의 `*ClientPort` 를 Spring `RestClient` + `@HttpExchange` 인터페이스 프록시로 구현한다. Bucket4j 토큰 버킷으로 Riot rate limit 보호. 실제 API 호출 / Fake 어댑터 (테스트/로컬용) 는 `@ConditionalOnProperty` 로 분기.

## Boundaries

- 허용: `core:lol-server-domain`, `spring-boot-starter-web` (RestClient/HttpExchange), `mapstruct`, `bucket4j-core`
- 금지: 다른 인프라 모듈 (DB, Redis, OAuth) 직접 호출 — 어댑터는 외부 HTTP 만 담당
- 외부 응답 모델 (`*VO`) 은 이 모듈 안에서만 사용. 도메인은 `ChampionRotate`, `Summoner` 등 도메인 객체만 알면 된다

## Layout

- `restclient/<domain>/Xxx*RestClient.java` — `@HttpExchange` 인터페이스 (메서드 시그니처가 곧 HTTP 호출)
- `restclient/<domain>/model/*VO.java` — Riot/외부 API 응답 DTO (jackson 직렬화 대상)
- `mapper/<domain>/Xxx*ClientMapper.java` — VO → 도메인 객체 MapStruct 매퍼
- `adapter/<domain>/Xxx*ClientAdapter.java` — `@Component implements XxxClientPort` (RestClient + Mapper 조합)
- `adapter/<domain>/FakeXxx*Adapter.java` — `@ConditionalOnProperty` 로 켜는 가짜 구현 (Bucket4j 로 throttle)
- `config/RestClientConfig.java` — `RestClient` 빈 + `HttpServiceProxyFactory` 로 인터페이스 → 빈 변환
- `error/RestClientException.java`, `error/ErrorType.java`, `error/ErrorCode.java`, `error/ErrorMessage.java` — 외부 API 호출 전용 예외/에러 enum (도메인 `CoreException` 과 별개)

## Key Files

- `adapter/champion/ChampionClientAdapter.java` — 가장 단순한 어댑터 reference (`@ConditionalOnProperty matchIfMissing=true`)
- `adapter/champion/FakeChampionClientAdapter.java` — Fake + Bucket4j throttle reference (테스트/로컬에서 Riot API 호출 회피)
- `restclient/summoner/SummonerRestClient.java` — `@HttpExchange` + `@GetExchange` 패턴 (BaseURL 은 RestClientConfig)
- `config/RestClientConfig.java` — `RestClient` 베이스 (timeout, base URL, 4xx → `RestClientException` 변환), `HttpServiceProxyFactory.createClient(...)` 로 인터페이스 빈 등록
- `error/RestClientException.java` — 외부 API 4xx/5xx 시 던지는 예외. 도메인의 `CoreException` 과 다른 계층이지만 비슷한 구조

## Common Modifications

- **새 Riot API 엔드포인트 추가**:
  1. `restclient/<domain>/Xxx*RestClient.java` 인터페이스에 `@GetExchange` 메서드 추가
  2. 응답 DTO `restclient/<domain>/model/Xxx*VO.java` 정의
  3. `mapper/<domain>/Xxx*ClientMapper.java` 에 `toDomain(VO)` 추가
  4. `adapter` 에서 메서드 호출 → 매퍼 → 도메인 반환
  5. 새 RestClient 인터페이스라면 `RestClientConfig` 에 `@Bean` 추가 (`HttpServiceProxyFactory.createClient(...)`)
- **타임아웃 변경**: `RestClientConfig.clientHttpRequestFactory()` 의 connect/read 타임아웃 (현재 1s/5s)
- **Fake 모드 활성화**: 프로퍼티 `<domain>.client.fake.enabled=true` → `Fake*Adapter` 가 우선 주입

## Failure Patterns / Gotchas

- ❌ 어댑터에서 VO 를 그대로 반환 — 도메인이 외부 API 모양에 결합됨
  ✅ Mapper 로 도메인 객체 변환 후 반환
- ❌ 4xx 처리 안 하고 응답 그대로 매핑 — null/빈 객체로 도메인 진입
  ✅ `RestClientConfig.defaultStatusHandler` 가 4xx 를 `RestClientException(EXTERNAL_API_ERROR)` 로 변환 — 도메인은 이 예외를 받아 `CoreException` 으로 재던지거나 retry
- ❌ `@HttpExchange` 인터페이스 빈 등록 누락 — `NoSuchBeanDefinitionException`
  ✅ `RestClientConfig` 에 `@Bean` 으로 명시 등록 (Spring 이 자동 스캔 안 함)
- ❌ Riot API 호출에 rate limit 보호 없음 — 운영에서 Riot 측 차단
  ✅ Bucket4j `Bucket.builder().addLimit(Bandwidth.of(...))` 로 throttle (`FakeChampionClientAdapter` 패턴 참조)
- ❌ `FAIL_ON_UNKNOWN_PROPERTIES = true` 로 외부 API 신규 필드에 깨짐
  ✅ `clientObjectMapper()` 가 명시적으로 `false` 로 설정 — 외부 ObjectMapper 와 분리해 사용

## Cross-Module Dependencies

- depends on: `core:lol-server-domain` (out port `*ClientPort`, 도메인 객체)
- consumed by: `app:application` (런타임 빈 주입). 다른 인프라는 직접 의존 안 함
- 외부 의존: `lol.repository.url` (게이트웨이/프록시 URL) — 환경별 yaml 설정

## Quick Commands

```bash
./gradlew :module:infra:client:lol-repository:test            # RestClient + Mapper + Fake 어댑터 테스트
./gradlew :module:infra:client:lol-repository:checkstyleMain  # checkstyle 단독
```

## See Also

- [core:lol-server-domain](../../core/lol-server-domain/CLAUDE.md) — `*ClientPort` 정의처
- [client/oauth](../oauth/CLAUDE.md) — 같은 client 카테고리, 인증/토큰 교환 담당
- [persistence/redis](../../persistence/redis/CLAUDE.md) — `SummonerCacheAdapter` 가 외부 호출 전 캐시 hit 검증
