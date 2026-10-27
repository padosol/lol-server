# app:application

Spring Boot 진입점 + 컴포지션 루트. **모든 인프라 모듈을 implementation 의존**으로 끌어와 빈 그래프를 완성하고 `bootJar` 를 만든다. 비즈니스 로직 없음 — 진입 클래스 1개와 cross-cutting 설정만.

## Boundaries

- 허용: `core:lol-server-domain`, `core:enum`, 모든 `infra:*`, `support:logging`, `spring-boot-starter-actuator`, `micrometer-registry-prometheus`, postgresql 드라이버 (runtimeOnly 전이 안 되어 직접 추가)
- 금지: 도메인 로직, 컨트롤러, 어댑터 — 이 모듈은 "조립" 만 한다
- `bootJar.enabled = true`, `jar.enabled = false` — 다른 모듈은 plain jar 로 빌드, 이 모듈만 실행 가능한 fat jar

## Layout

- `LolServerApplication.java` — `@SpringBootApplication @EnableAsync @EnableScheduling` 메인
- `config/ExecutorConfig.java` — `taskExecutor` (`ThreadPoolTaskExecutor`), `schedulerTask` (`ThreadPoolTaskScheduler`) 빈
- `config/DuoPostExpirationScheduler.java`, `config/OrphanImageCleanupScheduler.java` — 듀오 글 만료·고아 이미지 정리 `@Scheduled`
- `src/main/resources/application-{local,prod}.yml` — 프로파일별 모듈 yaml import 카탈로그

## Key Files

- `LolServerApplication.java` — main, 패키지 베이스 (`com.example.lolserver`) 컴포넌트 스캔의 출발점
- `src/main/resources/application-{local,prod}.yml` — 프로파일별 모듈 yaml import 카탈로그
- `src/main/resources/client-repository-{local,prod}.yml` — lol-repository 내부 API 주소 (`lol.repository.internal-url`, prod `LOL_REPOSITORY_INTERNAL_URL` = 내부 포트 8081)
- `config/ExecutorConfig.java` — 일반 `@Async`/`@Scheduled` 용 풀. (Virtual Thread 풀 `queryExecutor` 는 `common:config:AsyncQueryConfig` 에 별도 정의)
- `build.gradle` — 모듈 결합 spec 의 single source of truth (`bootJar { enabled = true }`)
- `src/main/resources/db/migration/app` — app 스키마(member·community·duo) Flyway 체인. lol-server 가 가진 DDL 은 이것뿐이고 app 밖 데이터는 lol-repository API 로 받는다. `AppSchemaMigrationTest`(Testcontainers)가 엔티티와 대조. 운영도 기동 때 app 스키마를 만들고 V1 부터 적용한다 — public 의 예전 데이터는 옮기지 않는다 (`docs/db-split/README.md`)

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
- ❌ Riot 데이터(소환사·전적·랭킹·게임 데이터) 테이블에 엔티티·쿼리 추가 — lol-server 는 app 스키마 밖에 DB 권한이 없다
  ✅ lol-repository API 로 받는다 (duo 는 `/internal/v1/**`). Hibernate validate 도 현재 스키마(`app`)만 본다
- ❌ 적용된 app 마이그레이션(V1·V2…) 수정 — 체크섬 불일치로 운영 기동 실패
  ✅ 새 버전으로 추가. 주석도 체크섬에 들어가므로 주석만 고치는 것도 안 된다
- ❌ 비즈니스 로직을 `LolServerApplication` 또는 이 모듈의 config 에 작성 — 헥사고날 위반
  ✅ 도메인 로직은 `core:lol-server-domain`, 어댑터는 `infra:*` 로

## Cross-Module Dependencies

- depends on (implementation): 모든 모듈 (`common`, `shared`, `support:logging`, `domain:{championstats,member,community,duo}`) — 단일 진실원천은 `build.gradle`
- consumed by: 없음 (terminal, bootJar 산출)
- 빌드: `./gradlew bootRun -Dspring.profiles.active=local` — Postgres/Redis Docker 서비스 필요

## Quick Commands

```bash
./gradlew :module:app:application:bootRun -Dspring.profiles.active=local  # local 부트 (Docker 서비스 필요)
./gradlew :module:app:application:bootJar                                 # 실행 가능 fat jar
./gradlew :module:app:application:test                                    # 모듈 단위 테스트

# DevTools 자동 재시작 — bootRun 은 컴파일된 build/classes 디렉터리를 감시하므로,
# 소스 저장 시 자동 재컴파일이 있어야 재시작이 걸린다. 둘 중 하나:
#  1) 별도 터미널에서 지속 컴파일 (전 모듈 소스 변경 감지)
./gradlew classes -t
#  2) IntelliJ: "Build project automatically" ON +
#     Advanced Settings → "Allow auto-make to start even if developed application is currently running"
```

> DevTools 는 `developmentOnly` 의존이라 `bootJar`(dev/prod) 에는 포함되지 않는다. 재시작/LiveReload 세부 설정은 `application-local.yml` 의 `spring.devtools.*` 에서.

## See Also

- [Root CLAUDE.md](../../../CLAUDE.md) — 전체 컴퍼스, build/run 명령어
- 각 모듈 CLAUDE.md (의존 그래프 확인)
- `src/main/resources/application.yml` — 활성 프로파일별 yaml import 목록
