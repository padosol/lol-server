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

# 클린 빌드
./gradlew clean build

```

## 기술 스택

- Java 17, Spring Boot 3.3.6, Gradle 8.5
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
│   ├── client/lol-repository/    # Riot API 클라이언트 (피동 어댑터)
│   ├── message/rabbitmq/         # 메시지 생산자/소비자
│   └── persistence/
│       ├── postgresql/           # JPA 엔티티, 리포지토리, 어댑터
│       └── redis/                # 캐싱 설정
└── support/logging/              # 횡단 관심사 유틸리티
```

### 핵심 아키텍처 규칙

**`core:lol-server-domain`은 인프라로부터 독립적이어야 합니다.**

- 허용: `core:enum`, 표준 Java/Jakarta, Lombok, 순수 인터페이스 (포트)
- 금지: 모든 `infra:*` 모듈, Spring Data, `@Entity`, 리포지토리 구현체

의존성은 항상 안쪽으로만 흐릅니다: `infra → core`, 절대로 `core → infra` 불가.

### 도메인 컨텍스트

각 도메인 (champion, league, match, patchnote, queue_type, rank, spectator, summoner, tiercutoff, version)은 다음 구조를 따릅니다:
- `domain/` - 순수 도메인 객체 (Write Model)
- `application/` - 애플리케이션 서비스
- `application/port/` - 포트 인터페이스 (in/out)
- `application/dto/` - Response DTO (Read Model)
- `application/model/` - 명시적 ReadModel 클래스

### Read Model 패턴

이 프로젝트는 **Write Model과 Read Model을 명확히 분리**합니다.

#### Write Model (도메인 엔티티)
- 위치: `domain/{도메인}/domain/`
- 비즈니스 로직 포함
- 상태 변경 가능

#### Read Model (DTO/Response)
- 위치: `domain/{도메인}/application/dto/` 또는 `application/model/`
- 표현 전용, 비즈니스 로직 없음
- 불변 (Java Record 권장)

#### 계층별 Read Model

| 계층 | 패키지 | 명명 규칙 | 용도 |
|------|--------|----------|------|
| 도메인 | `application/dto/` | `*Response` | 서비스 반환값 |
| 도메인 | `application/model/` | `*ReadModel` | 외부 API 조회 결과 |
| API | `controller/*/response/` | `*Response` | API 응답 전용 |
| 영속성 | `repository/*/dto/` | `*DTO` | QueryDSL 조회 결과 |

#### Read Model 생성 방식

- **팩토리 메서드** (권장): `SummonerResponse.of(Summoner)` - Builder 패턴으로 도메인→DTO 변환
- **Java Record**: 외부 API 조회 결과용 불변 객체 (예: `CurrentGameInfoReadModel`)
- **QueryDSL Projection**: `@QueryProjection` 생성자로 DB→DTO 직접 매핑

### 패키지 명명 규칙

- 도메인: `com.example.lolserver.domain.{domainName}`
- 도메인 포트: `com.example.lolserver.domain.{domainName}.application.port`
- 인프라 어댑터: `com.example.lolserver.repository.{domainName}.adapter`
- 인프라 매퍼: `com.example.lolserver.repository.{domainName}.mapper`
- 컨트롤러: `com.example.lolserver.controller.{domainName}`
- 컨트롤러 응답: `com.example.lolserver.controller.{domainName}.response`
- 컨트롤러 매퍼: `com.example.lolserver.controller.{domainName}.mapper`

### 클래스 작성요령

| 계층 | 접미사 | 예시 | 위치 |
|------|--------|------|------|
| 도메인 응답 DTO | `*Response` | `GameResponse` | `application/dto/` |
| 도메인 ReadModel | `*ReadModel` | `CurrentGameInfoReadModel` | `application/model/` |
| 컨트롤러 응답 | `*Response` | `SliceResponse` | `controller/*/response/` |
| 영속성 DTO | `*DTO` | `MSChampionDTO` | `repository/*/dto/` |
| 엔티티 | `*Entity` | `MatchEntity` | `repository/*/entity/` |
| 커맨드 | `*Command` | `MatchCommand` | `application/command/` |

### API 응답 래퍼 패턴

모든 API 응답은 `ApiResponse<T>` 래퍼로 감쌉니다:
- `ApiResponse.success(data)` - 성공 응답
- `ApiResponse.error(errorType)` - 에러 응답
- 페이지네이션: `SliceResponse<T>` (Spring `Page<T>` 대신 커스텀 `Page<T>` 사용)

### 에러 처리

- `CoreException(ErrorType)` - 비즈니스 예외 (RuntimeException 상속)
- `ErrorType` enum - HTTP 상태 코드 매핑
- `@RestControllerAdvice(CoreExceptionAdvice)` - 전역 예외 핸들러

### 테스트 패턴

- **단위 테스트**: `@ExtendWith(MockitoExtension.class)`, BDDMockito (`given/then`)
- **JPA 테스트**: `RepositoryTestBase` 상속 (`@DataJpaTest` + H2)
- **RestDocs 테스트**: `RestDocsSupport` 상속 (Standalone MockMvc)
- **어댑터 테스트**: Mock 기반 단위 테스트 (통합 테스트 아님)
- `@DisplayName("한글 설명")`, AssertJ `assertThat` 사용

### 코드 컨벤션

- DI: `@RequiredArgsConstructor` + `private final` 필드 (생성자 주입)
- 로깅: `@Slf4j`
- 컨트롤러 응답 DTO: Java `record` (불변)
- 커맨드: `@Builder @Getter @Setter @NoArgsConstructor @AllArgsConstructor`
- 트랜잭션: 조회 `@Transactional(readOnly = true)`, 변경 `@Transactional`

### 커밋 메시지 컨벤션

- 형식: `<type>: <한글 설명>`
- 타입: `feat`, `fix`, `refactor`, `docs`, `chore`
- 예시: `feat: 소환사별 매치 목록 배치 조회 API 추가`

## 설정

애플리케이션 설정은 모듈별 YAML 파일에서 가져옵니다:
- `application.yml` imports: `core-local.yml`, `api-local.yml`, `client-repository-local.yml`, `rabbitmq-local.yml`, `postgresql-local.yml`, `redis-local.yml`
- 프로파일: `local`, `dev`, `prod`, `test`

Riot API 키 설정: `riot.api.key` 속성

### 참조 문서

TDD 진행 시 다음 스킬을 참조하여 패턴 준수:
- `.claude/skills/test-driven-development/SKILL.md` - TDD 워크플로우 및 패턴
- `.claude/skills/test-driven-development/testing-anti-patterns.md` - 테스트 안티패턴
- `.claude/skills/build-validator/SKILL.md` - 빌드 오류 분석