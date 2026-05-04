# app:application

Spring Boot 진입점 + 컴포지션 루트. **모든 인프라 모듈을 implementation 의존**으로 끌어와 빈 그래프를 완성하고 `bootJar` 를 만든다. 비즈니스 로직 없음 — 진입 클래스 1개와 cross-cutting 설정만.

## Boundaries

- 허용: `core:lol-server-domain`, `core:enum`, 모든 `infra:*`, `support:logging`, `spring-boot-starter-actuator`, `micrometer-registry-prometheus`, postgresql 드라이버 (runtimeOnly 전이 안 되어 직접 추가)
- 금지: 도메인 로직, 컨트롤러, 어댑터 — 이 모듈은 "조립" 만 한다
- `bootJar.enabled = true`, `jar.enabled = false` — 다른 모듈은 plain jar 로 빌드, 이 모듈만 실행 가능한 fat jar

## Layout

- `LolServerApplication.java` — `@SpringBootApplication @EnableAsync @EnableScheduling` 메인
- `config/ExecutorConfig.java` — `taskExecutor` (`ThreadPoolTaskExecutor`), `schedulerTask` (`ThreadPoolTaskScheduler`) 빈
- `config/CacheConfig.java` — `@EnableCaching`, `ConcurrentMapCacheManager("rotation")` (in-memory, 노드 로컬)
- `config/CacheScheduler.java` — `@Scheduled` cron 으로 로테이션 캐시 evict
- `src/main/resources/application.yml` — 프로파일별 (`local`/`dev`/`prod`) 모듈 설정 import + RabbitMQ/Kafka AutoConfiguration exclude

## Key Files

- `LolServerApplication.java` — main, 패키지 베이스 (`com.example.lolserver`) 컴포넌트 스캔의 출발점
- `src/main/resources/application.yml` — 프로파일별 모듈 yaml import 카탈로그 + `message.broker` 기본값 (`rabbitmq`)
- `config/ExecutorConfig.java` — 일반 `@Async`/`@Scheduled` 용 풀. (Virtual Thread 풀 `queryExecutor` 는 `infra:persistence:postgresql:AsyncQueryConfig` 에 별도 정의)
- `config/CacheConfig.java` — `rotation` 캐시 정의 위치. 신규 캐시 추가 시 여기 cacheNames 추가
- `build.gradle` — 모듈 결합 spec 의 single source of truth (`bootJar { enabled = true }`)

## Common Modifications

- **새 인프라 모듈 추가**: `settings.gradle` 에 include + 이 모듈 `build.gradle` 에 `implementation project(...)` 추가, `application.yml` 에 환경별 yaml import 등록
- **신규 글로벌 설정 (CORS/Cache/Async pool)**: 가능한 가장 가까운 인프라 모듈 (`infra:api`, `infra:persistence:postgresql` 등) 에 두고, 진짜 cross-cutting 만 여기 둘 것
- **프로파일/환경 설정**: 각 모듈의 `src/main/resources/<module>-<profile>.yml` 을 만들고 이 모듈의 `application.yml` 에서 import
- **Actuator/Prometheus 엔드포인트 변경**: 환경 yaml 의 `management.*` 에서 — 이 모듈의 dependency 만 추가되어 있음

## Failure Patterns / Gotchas

- ❌ runtimeOnly 의존성 (DB 드라이버 등) 이 인프라 모듈에 잡혔다고 안심 — Gradle 은 runtimeOnly 를 전이하지 않음
  ✅ `app/application/build.gradle` 에 `runtimeOnly 'org.postgresql:postgresql'` 처럼 직접 추가 (이미 패턴 있음)
- ❌ `@SpringBootApplication` 패키지보다 위쪽에 `@Component` 추가 — 컴포넌트 스캔 누락
  ✅ 모든 어댑터/서비스는 `com.example.lolserver.*` 하위에 둘 것 (멀티 모듈이지만 베이스 패키지 통일)
- ❌ Kafka/RabbitMQ 둘 다 활성화 — 같은 `*MessagePort` 에 여러 빈 등록
  ✅ `application.yml` 의 `message.broker` 가 `kafka` 또는 `rabbitmq` 중 하나만 — 어댑터 측 `@ConditionalOnProperty` 로 자동 분기
- ❌ broker 어댑터 미존재 환경에서 부팅 — `RabbitAutoConfiguration` 이 connection 시도하다 실패
  ✅ `application.yml` 의 `spring.autoconfigure.exclude` 에 두 AutoConfiguration 모두 명시 (이미 처리됨)
- ❌ 비즈니스 로직을 `LolServerApplication` 또는 이 모듈의 config 에 작성 — 헥사고날 위반
  ✅ 도메인 로직은 `core:lol-server-domain`, 어댑터는 `infra:*` 로

## Cross-Module Dependencies

- depends on (implementation): 모든 모듈 (`core:lol-server-domain`, `core:enum`, `infra:api`, `infra:client:lol-repository`, `infra:client:oauth`, `infra:message:rabbitmq`, `infra:message:kafka`, `infra:persistence:postgresql`, `infra:persistence:redis`, `infra:persistence:clickhouse`, `support:logging`)
- consumed by: 없음 (terminal, bootJar 산출)
- 빌드: `./gradlew bootRun -Dspring.profiles.active=local` — Postgres/Redis/RabbitMQ 등 Docker 서비스 필요

## See Also

- [Root CLAUDE.md](../../../CLAUDE.md) — 전체 컴퍼스, build/run 명령어
- 각 모듈 CLAUDE.md (의존 그래프 확인)
- `src/main/resources/application.yml` — 활성 프로파일별 yaml import 목록
