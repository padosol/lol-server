# LoL Server

리그 오브 레전드 전적 검색 서비스의 회원·커뮤니티·듀오 백엔드. OAuth/RSO 로그인, 커뮤니티, 듀오 찾기와 (이전 전까지) 챔피언 통계를 제공합니다.
Riot 데이터(소환사·전적·리그/랭킹·게임 데이터)는 lol-repository 가 소유하고 서빙하며, lol-server 는 DB 가 아니라 그 API 로만 받습니다 (app 스키마만 소유 — [`docs/db-split/README.md`](docs/db-split/README.md)).

## 기술 스택

- **Java 21** (Gradle Toolchain), Spring Boot 3.3.6, Gradle Multi-Module
- **PostgreSQL** + Flyway (영속·timeline 원본), **Redis / Redisson** (캐시 / RefreshToken / OAuth State / 분산 락)
- **BigQuery** (챔피언 통계 OLAP — `STATS_DATASOURCE=bigquery`)
- QueryDSL 5.1.0, MapStruct (객체 매핑), Spring Security + OAuth2 (RSO), Resilience4j (lol-repository 호출 서킷 브레이커)
- Spring RestDocs + Asciidoctor (API 문서화), JaCoCo (커버리지), Checkstyle 10.21.4

## 시작하기

### 요구사항

- Java 21+
- Docker (인프라 / 모니터링 스택용)

### 인프라 의존

`local` 프로파일은 다음 외부 서비스가 필요합니다.

- PostgreSQL: `localhost:5432` (DB `postgres`, user `postgres`, password `1234` 기본)
- Redis: `localhost:6379`
- (선택) lol-repository 내부 API: `http://localhost:8112` — 듀오 글·신청에 붙일 Riot 계정 통계 조회. 없으면 통계 없이 만들어진다
- BigQuery 자격증명 (`stats.datasource=bigquery` 사용 시)

로컬 인프라는 `docker/docker-compose-local.yml` 로 한 번에 기동할 수 있습니다.

```bash
# postgres / redis
docker compose -f docker/docker-compose-local.yml up -d
```

모니터링 스택 (Prometheus / Grafana / Loki / Promtail + 앱 컨테이너 통합) 은 별도 compose 로 분리되어 있습니다.

```bash
docker compose -f docker/docker-compose.monitoring.yml up -d
# Grafana http://localhost:3010 (admin/admin), Prometheus :9090, Loki :3100
```

> 운영 이미지는 `docker/Dockerfile` (JDK 21 멀티스테이지 빌드).

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

- 활성 프로파일: `local`, `prod`, `test`
- 모듈별 yaml (`<module>-<profile>.yml`) 을 `module/app/application/src/main/resources/application-<profile>.yml` 에서 import
- 주요 환경변수: `STATS_DATASOURCE=bigquery`, `DB_*`, `LOL_REPOSITORY_INTERNAL_URL`(prod, lol-repository 내부 포트 8081), RSO 클라이언트 자격증명, BigQuery 서비스 계정 경로

## 아키텍처

**헥사고날 (Ports & Adapters)** 기반의 Gradle 멀티 모듈 구조. 컨텍스트마다 `domain` + `application`(port.in·port.out) + `adapter`(in/web · out) 를 갖고, 의존은 adapter → application → domain 단방향입니다. `app:application` 만 `bootJar` 를 산출하며 모든 모듈을 implementation 으로 묶어 컴포지션 루트 역할을 합니다.

```
module/
├── app/application/                 # Spring Boot 진입점, 컴포지션 루트, bootJar, app 스키마 Flyway 체인
├── common/                          # 공유 커널 (web·error·security + JPA·Redis·S3 config, testFixtures)
├── shared/                          # Tier, Platform, TierFilter (championstats 용)
├── support/logging/                 # @LogExecutionTime AOP 등 횡단 유틸
└── domain/
    ├── member/                      # 회원 + RSO/OAuth2 로그인 (RefreshToken / JWT)
    ├── community/                   # 게시글 / 댓글 / 투표 / 이미지
    ├── duo/                         # 듀오 모집글 / 신청 (Riot 계정 통계는 lol-repository 내부 API)
    └── championstats/               # 챔피언 통계 OLAP (BigQuery) — lol-repository 로 이전 예정
```

자세한 모듈 가이드는 [`CLAUDE.md`](CLAUDE.md) 와 각 모듈의 `CLAUDE.md` 참조.

### 데이터 소스 의사결정

- **lol-server 는 app 스키마(회원·커뮤니티·듀오)만 소유한다.** Riot 데이터 테이블에 엔티티·쿼리를 만들지 않고 lol-repository API 로 받는다.
- **lol-server 는 lol-repository 에 종속되지 않는다.** 듀오의 Riot 계정 통계(티어·모스트 챔피언·최근 게임)는 `GET /internal/v1/summoners/{puuid}/duo-profile`(lol-repository 내부 포트)로 받아 글·신청 생성 시점에 스냅샷으로 저장하되, 받지 못하면(무응답·장애·색인 없음) 통계 없이 만든다(티어 `UNRANKED` 표식, 언랭 검증 생략). 듀오는 RSO 연동 회원만 쓰므로 매칭 상대에게 보여 줄 Riot ID 는 lol-repository 에 묻지 않고 연동 때 저장한 값을 쓴다. 그 값이 없는 예전 연동이면 다시 연동하라고 막는다(`RIOT_ACCOUNT_RELINK_REQUIRED`).
- **챔피언 통계 OLAP 은 BigQuery** (`STATS_DATASOURCE=bigquery`). lol-repository 로 이전 예정.

## 코드 컨벤션 요약

상세 규칙은 각 모듈 `CLAUDE.md`. 공통 베이스라인:

- DI: `@RequiredArgsConstructor` + `private final` (생성자 주입)
- 트랜잭션: 조회 `@Transactional(readOnly = true)`, 변경 `@Transactional`
- API 응답: `ResponseEntity<ApiResponse<T>>`, RESTful 상태 코드 (POST 201 / GET·PUT 200 / DELETE 204)
- 도메인 규칙은 도메인 객체의 `validate*` guard 가 직접 던진다 (서비스에서 boolean+throw 금지)
- ReadModel 변환은 `*ReadModel.of(domain)` 정적 팩토리에서만
- 매직 스트링 금지: `OAuthProvider.RIOT.name()`, `DuoPostStatus.ACTIVE.name()` 등 enum 사용
- 커밋 메시지: `<type>: <한글 설명>` (타입 `feat`, `fix`, `refactor`, `docs`, `chore`)
- 브랜치: `feature/*`, `fix/*`, `refactor/*` (`<type>/<kebab 설명>`) → `develop` → `main`; hotfix 는 `hotfix/* → main → develop`
- Linear 이슈 연결: 브랜치·커밋이 아니라 PR 본문에 `Closes MP-<번호>` (일부만 다루면 `Part of MP-<번호>`)

## 문서

- [`CLAUDE.md`](CLAUDE.md) — 모듈 라우팅 / 작업 별 진입점
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — 모듈 의존 그래프 + 데이터 흐름 (mermaid) + 변경 영향 표
- [`docs/workflow.md`](docs/workflow.md) — 브랜치 / 커밋 규칙, PR 본문 기반 Linear (`MP-*`) 이슈 연결 · 생명주기
- [`docs/docs-maintenance.md`](docs/docs-maintenance.md) — CLAUDE.md / 문서 동기화 절차 (PR 시점 / 분기 리뷰 / CI 게이트)
- [`docs/oauth2-login.md`](docs/oauth2-login.md), [`docs/rso-oauth2-troubleshooting.md`](docs/rso-oauth2-troubleshooting.md) — OAuth/RSO 흐름 디테일
- `module/infra/api/src/docs/asciidoc/index.adoc` — 생성된 API 문서 entry

## 라이선스

이 프로젝트는 비공개 저장소입니다.
