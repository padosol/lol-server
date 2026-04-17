# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

리그 오브 레전드 전적 검색 서비스 - Riot Games API를 통해 게임 통계, 소환사 프로필, 매치 데이터를 제공하는 Spring Boot 백엔드 애플리케이션.

## 빌드 및 실행 명령어

```bash
# 프로젝트 빌드
./gradlew build

# 로컬 실행 (Docker 서비스 필요)
./gradlew bootRun -Dspring.profiles.active=local

# 테스트 실행
./gradlew test

# API 문서 생성 (RestDocs 테스트 성공 후 asciidoctor 실행)
./gradlew :module:infra:api:asciidoctor

# 클린 빌드
./gradlew clean build

```

## 기술 스택

- Java 21, Spring Boot 3.3.6, Gradle 8.5
- PostgreSQL (영속성), Redis/Redisson (캐싱), RabbitMQ (메시징)
- QueryDSL 5.1.0, MapStruct 1.5.5, Bucket4j (Rate Limiting)
- Spring RestDocs (API 문서화)

## 아키텍처

이 프로젝트는 **헥사고날 아키텍처 (Ports & Adapters)** 를 구현합니다.

### 모듈 구조

```
module/
├── app/application/              # 진입점, 컴포지션 루트 (모든 모듈 의존)
├── core/
│   ├── lol-server-domain/        # 도메인 계층 + 애플리케이션 서비스 + 포트
│   └── enum/                     # 공유 enum 타입
├── infra/
│   ├── api/                      # REST 컨트롤러 (구동 어댑터)
│   ├── client/
│   │   ├── lol-repository/       # Riot API 클라이언트 (피동 어댑터)
│   │   └── oauth/                # OAuth2 클라이언트 (토큰 교환, RSO)
│   ├── message/
│   │   ├── rabbitmq/             # RabbitMQ 메시지 생산자/소비자
│   │   └── kafka/                # Kafka 메시지 브로커 어댑터
│   └── persistence/
│       ├── postgresql/           # JPA 엔티티, 리포지토리, 어댑터
│       ├── redis/                # 캐싱, RefreshToken, OAuth State 저장
│       └── clickhouse/           # 분석용 데이터 저장소
└── support/logging/              # 횡단 관심사 유틸리티
```

### 핵심 아키텍처 규칙

**`core:lol-server-domain`은 인프라로부터 독립적이어야 합니다.**

- 허용: `core:enum`, 표준 Java/Jakarta, Lombok, 순수 인터페이스 (포트)
- 금지: 모든 `infra:*` 모듈, Spring Data, `@Entity`, 리포지토리 구현체

의존성은 항상 안쪽으로만 흐릅니다: `infra → core`, 절대로 `core → infra` 불가.

### 도메인 컨텍스트

각 도메인(`module/core/lol-server-domain/.../domain/` 하위)은 다음 구조를 따릅니다:
- `domain/` - 순수 도메인 객체 (Write Model, 비즈니스 로직 포함)
- `application/` - 애플리케이션 서비스
- `application/port/` - 포트 인터페이스 (in/out)
- `application/dto/` - Command, SearchDto 등 입력 DTO
- `application/model/` - ReadModel (불변, Java Record, 팩토리 메서드 `*.of()` 변환)

### 클래스 명명 규칙

| 계층 | 접미사 | 예시 | 패키지 |
|------|--------|------|--------|
| 도메인 ReadModel | `*ReadModel` | `GameReadModel` | `domain.{name}.application.model` |
| 컨트롤러 응답 | `*Response` | `SliceResponse` | `controller.{name}.response` |
| 영속성 DTO | `*DTO` | `MSChampionDTO` | `repository.{name}.dto` |
| 엔티티 | `*Entity` | `MatchEntity` | `repository.{name}.entity` |
| 어댑터 | `*Adapter` | `MatchPersistenceAdapter` | `repository.{name}.adapter` |
| 매퍼 | `*Mapper` | `MatchMapper` | `repository.{name}.mapper` |
| 커맨드 | `*Command` | `MatchCommand` | `domain.{name}.application.command` |

### API 응답 래퍼 패턴

모든 API 응답은 `ApiResponse<T>` 래퍼로 감쌉니다:
- `ApiResponse.success(data)` - 성공 응답
- `ApiResponse.error(errorType)` - 에러 응답
- 페이지네이션: `SliceResponse<T>` (Spring `Page<T>` 대신 커스텀 `Page<T>` 사용)

### 에러 처리

- `CoreException(ErrorType)` - 비즈니스 예외 (RuntimeException 상속)
- `ErrorType` enum - HTTP 상태 코드 매핑
- `@RestControllerAdvice(CoreExceptionAdvice)` - 전역 예외 핸들러

### 도메인 검증 패턴

도메인 규칙 위반은 **도메인 객체가 직접 예외를 던져야** 합니다. Application 서비스에서 boolean 체크 후 예외를 던지지 않습니다.

```java
// ✅ 올바른 패턴: 도메인 객체의 guard 메서드
duoPost.validateOwner(memberId);
duoPost.validateActive();
member.validateNotWithdrawn();

// ❌ 금지: Application 서비스에서 boolean 체크 + 예외 던지기
if (!duoPost.isOwner(memberId)) {
    throw new CoreException(ErrorType.FORBIDDEN);
}
```

- guard 메서드 네이밍: `validate*` 접두사 (`validateOwner`, `validateActive`, `validateNotDeleted` 등)
- boolean 쿼리 메서드(`isOwner`, `isActive` 등)는 조건 분기용으로만 사용하고, 불변식 강제에는 guard 메서드 사용
- 동일 boolean에 대해 반대 의미의 guard가 필요하면 별도 메서드 분리 (`validateOwner` vs `validateNotOwner`)

### 테스트 패턴

- **단위 테스트**: `@ExtendWith(MockitoExtension.class)`, BDDMockito (`given/then`)
- **JPA 테스트**: `RepositoryTestBase` 상속 (`@DataJpaTest` + H2)
- **RestDocs 테스트**: `RestDocsSupport` 상속 (Standalone MockMvc)
- **어댑터 테스트**: Mock 기반 단위 테스트 (통합 테스트 아님)
- **MapStruct Mapper 테스트**: 새 Mapper 추가 또는 기존 Mapper 수정 시 반드시 단위 테스트 작성
  - 테스트 위치: `repository.{name}.mapper.*MapperTest`
  - `componentModel = "spring"` Mapper: `new MapperImpl()` + `ReflectionTestUtils.setField()`로 의존 Mapper 주입
  - `componentModel = "default"` Mapper: `Mapper.INSTANCE` 사용
  - 필수 검증 항목: `@Mapping(ignore = true)` 필드가 실제로 무시되는지, `@AfterMapping`이 의도한 메서드에만 적용되는지
  - `updateEntityFromDomain` 테스트 시 기존 컬렉션에 중복 엔티티가 추가되지 않는지 검증
- `@DisplayName("한글 설명")`, AssertJ `assertThat` 사용
- RestDocs 관련 파일(`.adoc`, RestDocs 테스트) 수정 시 반드시 `./gradlew :module:infra:api:asciidoctor` 실행하여 문서 재생성할 것

### 코드 컨벤션

- DI: `@RequiredArgsConstructor` + `private final` 필드 (생성자 주입)
- 로깅: `@Slf4j`
- 컨트롤러 반환 타입: `ResponseEntity<ApiResponse<T>>` (RESTful 상태코드 사용)
  - `POST` (생성): `ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data))` → **201**
  - `GET` (조회): `ResponseEntity.ok(ApiResponse.success(data))` → **200**
  - `PUT` (수정): `ResponseEntity.ok(ApiResponse.success(data))` → **200**
  - `DELETE` (삭제): `ResponseEntity.noContent().build()` → **204** (body 없음, 반환 타입 `ResponseEntity<Void>`)
- 컨트롤러 응답 DTO: Java `record` (불변)
- 커맨드: `@Builder @Getter @NoArgsConstructor @AllArgsConstructor` (도메인 객체에 `@Setter` 사용 금지 — 팩토리 메서드/Builder 사용)
- 트랜잭션: 조회 `@Transactional(readOnly = true)`, 변경 `@Transactional`
- 매직 스트링 금지: 기존 enum이 존재하는 값은 반드시 `EnumType.VALUE.name()` 또는 `EnumType.VALUE.getXxx()` 사용
  - `OAuthProvider` (`core:lol-server-domain`): `"RIOT"`, `"GOOGLE"` 대신 `OAuthProvider.RIOT.name()`
  - `QueueType` (`core:enum`): `"RANKED_SOLO_5x5"` 대신 `QueueType.RANKED_SOLO_5x5.name()`, `420` 대신 `QueueType.RANKED_SOLO_5x5.getQueueId()`
  - `DuoPostStatus`, `DuoRequestStatus` 등 도메인 VO enum 동일 적용
- ReadModel 변환: 서비스에서 인라인 빌더로 ReadModel을 생성하지 말고, `ReadModel.of(DomainObject, ...)` 정적 팩토리 메서드를 ReadModel 클래스에 정의하여 사용

### 비동기 쿼리 실행 패턴

- Virtual Thread 기반 병렬 쿼리 실행 (`CompletableFuture.supplyAsync()` + `queryExecutor` 빈)
- `@LogExecutionTime` AOP 어노테이션으로 메서드 실행 시간 로깅

### Git 워크플로우

**브랜치 전략**: Git Flow 변형 — `feature/*`, `fix/*`, `refactor/*` → `develop` → `main`
**Hotfix 플로우**: `hotfix/*` → `main` → `develop` 역반영

> Jira 키(`MP-*`)를 포함한 브랜치 네이밍, 커밋 규칙, 티켓 생명주기는 [`docs/workflow.md`](docs/workflow.md) 참조.

### 커밋 메시지 컨벤션

- 형식: `<type>: MP-<번호> <한글 설명>` (Jira 키 필수)
- 타입: `feat`, `fix`, `refactor`, `docs`, `chore`
- 예시: `feat: MP-1 소환사별 매치 목록 배치 조회 API 추가`

## 설정

애플리케이션 설정은 모듈별 YAML 파일에서 가져옵니다:
- `application.yml` imports: `core-local.yml`, `api-local.yml`, `client-repository-local.yml`, `rabbitmq-local.yml`, `postgresql-local.yml`, `redis-local.yml`
- 프로파일: `local`, `dev`, `prod`, `test`

Riot API 키 설정: `riot.api.key` 속성

### 참조 문서

- `.claude/skills/build-validator/SKILL.md` - 빌드 오류 분석