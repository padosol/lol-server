# LoL Server

리그 오브 레전드 전적 검색 서비스 백엔드. Riot Games API 를 통해 챔피언 통계, 매치 분석, 소환사 프로필, 리그/랭킹, OAuth/RSO 로그인 등을 제공합니다.

## 기술 스택

- **Java 21** (Gradle Toolchain), Spring Boot 3.3.6, Gradle Multi-Module
- **PostgreSQL** + Flyway (영속·timeline 원본), **Redis / Redisson** (캐시 / RefreshToken / OAuth State / 분산 락)
- **BigQuery** (챔피언 통계 OLAP — `STATS_DATASOURCE` 기본값) / ClickHouse (fallback, runtime dead by default)
- **RabbitMQ** (기본 메시지 broker — `message.broker=rabbitmq`) / Kafka (`message.broker=kafka` 시 활성)
- QueryDSL 5.1.0, MapStruct (객체 매핑), Bucket4j (Riot API rate limiting), Spring Security + OAuth2 (RSO)
- Spring RestDocs + Asciidoctor (API 문서화), JaCoCo (커버리지), Checkstyle 10.21.4

## 시작하기

### 요구사항

- Java 21+
- Docker (PostgreSQL / Redis / RabbitMQ 인프라용)
- Riot Games API Key

### 인프라 의존

`local` 프로파일은 다음 외부 서비스가 필요합니다.

- PostgreSQL: `localhost:5432` (DB `postgres`, user `postgres`, password `1234` 기본)
- Redis: `localhost:6379`
- RabbitMQ: `localhost:5672` (관리 UI: 15672)
- Riot API local proxy (lol-repository): `http://localhost:8111`
- BigQuery 자격증명 (`stats.datasource=bigquery` 사용 시)

> 현재 저장소에 `docker-compose` 매니페스트는 포함되어 있지 않습니다. 위 서비스는 별도로 기동하세요. (운영 컨테이너 이미지는 `docker/Dockerfile` — JDK 21 멀티스테이지 빌드)

### 빌드 및 실행

```bash
# 로컬 실행 (Docker 인프라 기동 후)
./gradlew bootRun -Dspring.profiles.active=local

# 단일 모듈 실행 가능 jar (bootJar 산출은 app:application 모듈에만 활성)
./gradlew :module:app:application:bootJar

# 전체 테스트
./gradlew test

# 단일 클래스만 실행
./gradlew :module:<모듈경로>:test --tests <클래스명>

# RestDocs HTML 재생성 (RestDocs 테스트 변경 시 필수)
./gradlew :module:infra:api:asciidoctor

# 클린 빌드
./gradlew clean build
```

`run-local.sh` 는 build → JAR 실행을 한 번에 수행하며, `.env` 의 `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD` 를 system property 로 주입합니다.

### 환경 설정

- 활성 프로파일: `local`, `dev`, `prod`, `test`, `performance-test`
- 모듈별 yaml (`<module>-<profile>.yml`) 을 `module/app/application/src/main/resources/application.yml` 에서 프로파일별로 import
- 주요 환경변수: `STATS_DATASOURCE` (`bigquery` 기본 / `clickhouse` fallback), `DB_*`, Riot/RSO 클라이언트 자격증명, BigQuery 서비스 계정 경로
- 메시지 broker 전환: `message.broker=rabbitmq` (기본) 또는 `kafka` — 한쪽만 활성. 어댑터에 `@ConditionalOnProperty` 가 걸려 있어 자동 분기

## 아키텍처

**헥사고날 (Ports & Adapters)** 기반의 Gradle 멀티 모듈 구조. 의존은 항상 `infra → core` 단방향이며 역방향(`core → infra`)은 금지. `app:application` 만 `bootJar` 를 산출하며 모든 인프라 모듈을 implementation 으로 묶어 컴포지션 루트 역할을 합니다.

```
module/
├── app/application/                 # Spring Boot 진입점, 컴포지션 루트, bootJar
├── core/
│   ├── lol-server-domain/           # 도메인 + 애플리케이션 서비스 + in/out 포트 (인프라 무지)
│   └── enum/                        # 공유 enum (QueueType, Tier, Platform 등)
├── infra/
│   ├── api/                         # REST 컨트롤러 + Spring Security/OAuth2 + RestDocs
│   ├── client/
│   │   ├── lol-repository/          # Riot API RestClient + @HttpExchange + Bucket4j
│   │   └── oauth/                   # RSO/OAuth2 토큰 교환 + 사용자 정보
│   ├── message/
│   │   ├── rabbitmq/                # 기본 메시지 broker (message.broker=rabbitmq)
│   │   └── kafka/                   # Kafka producer (message.broker=kafka 시 활성)
│   └── persistence/
│       ├── postgresql/              # JPA + QueryDSL + MapStruct + Flyway
│       ├── redis/                   # 캐시, RefreshToken, OAuth State, Redisson 분산 락
│       ├── bigquery/                # 챔피언 통계 OLAP (STATS_DATASOURCE=bigquery, default)
│       └── clickhouse/              # 챔피언 통계 OLAP fallback (runtime dead by default)
└── support/logging/                 # @LogExecutionTime AOP 등 횡단 유틸
```

자세한 모듈 가이드는 각 디렉토리의 `CLAUDE.md` 와 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) (의존 그래프 + 데이터 흐름 mermaid) 참조.

### 도메인 컨텍스트

`module/core/lol-server-domain` 의 주요 바운디드 컨텍스트.

- **champion / championstats**: 챔피언 메타정보 + 랭크/롤별 통계 (승률·픽률·밴률·룬·스펠·아이템·매치업)
- **summoner**: 소환사 프로필 (퍼즈 ID, riot ID, profile icon)
- **match**: 매치 기록·검색·타임라인 분석
- **league / rank / tiercutoff**: 리그·티어·랭킹·티어 컷오프
- **spectator**: 실시간 게임 정보
- **member**: 회원 + RSO/OAuth2 로그인 (RefreshToken / JWT 포함)
- **community**: 게시글 / 댓글 / 투표
- **duo**: 듀오 모집글 / 신청
- **patchnote / season / version / queue_type**: 메타 데이터 / 패치 노트

### 데이터 소스 / 메시징 의사결정

- **챔피언 통계 OLAP 은 BigQuery 가 default** (`STATS_DATASOURCE` 미지정 시 `bigquery` 로 채움). ClickHouse 어댑터·Config 코드는 잔존하지만 기본 환경에서는 **runtime dead path** — 신규 통계 기능은 BigQuery 우선으로 작성합니다 (port 일치 보장을 위해 양쪽에 동일 메서드 추가 권장). 모듈 제거 시점은 BigQuery 운영 안정성 확인 후 별도 결정.
- **Riot API 직접 호출은 `infra/client/lol-repository` 만 가능** — 도메인/다른 어댑터에서 직접 RestClient 호출 금지.
- **메시지 broker 는 RabbitMQ 가 default**. Kafka 는 `message.broker=kafka` 환경에서만 활성화.

## 코드 컨벤션 요약

상세 규칙은 각 모듈 `CLAUDE.md`. 공통 베이스라인:

- DI: `@RequiredArgsConstructor` + `private final` (생성자 주입)
- 트랜잭션: 조회 `@Transactional(readOnly = true)`, 변경 `@Transactional`
- API 응답: `ResponseEntity<ApiResponse<T>>`, RESTful 상태 코드 (POST 201 / GET·PUT 200 / DELETE 204)
- 도메인 규칙은 도메인 객체의 `validate*` guard 가 직접 던진다 (서비스에서 boolean+throw 금지)
- ReadModel 변환은 `*ReadModel.of(domain)` 정적 팩토리에서만
- 매직 스트링 금지: `OAuthProvider.RIOT.name()`, `QueueType.RANKED_SOLO_5x5.name()` 등 enum 사용
- 커밋 메시지: `<type>: MP-<번호> <한글 설명>` (Linear 키 필수; 타입 `feat`, `fix`, `refactor`, `docs`, `chore`)
- 브랜치: `feature/MP-<번호>-*`, `fix/MP-<번호>-*`, `refactor/MP-<번호>-*` → `develop` → `main`; hotfix 는 `hotfix/MP-<번호>-* → main → develop`

## 문서

- [`CLAUDE.md`](CLAUDE.md) — 모듈 라우팅 / 작업 별 진입점
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — 모듈 의존 그래프 + 데이터 흐름 (mermaid) + 변경 영향 표
- [`docs/workflow.md`](docs/workflow.md) — Linear (`MP-*`) 키 기반 브랜치 / 커밋 / 이슈 생명주기
- [`docs/docs-maintenance.md`](docs/docs-maintenance.md) — CLAUDE.md / 문서 동기화 절차 (PR 시점 / 분기 리뷰 / CI 게이트)
- [`docs/oauth2-login.md`](docs/oauth2-login.md), [`docs/rso-oauth2-troubleshooting.md`](docs/rso-oauth2-troubleshooting.md) — OAuth/RSO 흐름 디테일
- `docs/0[1-4]_*.sql` — ClickHouse 스키마 정의 / 뷰 / 쿼리 (legacy, 참고용)
- `module/infra/api/src/docs/asciidoc/index.adoc` — 생성된 API 문서 entry

## 라이선스

이 프로젝트는 비공개 저장소입니다.
