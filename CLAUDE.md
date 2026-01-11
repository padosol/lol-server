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

# 인프라 서비스 시작
cd docker && docker-compose up -d
# 서비스: PostgreSQL:5432, Redis:6379, RabbitMQ:5672 (관리UI:15672)
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

각 도메인 (champion, league, match, queue_type, rank, spectator, summoner)은 다음 구조를 따릅니다:
- `domain/` - 순수 도메인 객체
- `application/` - 애플리케이션 서비스
- `application/port/` - 포트 인터페이스 (in/out)

### 패키지 명명 규칙

- 도메인: `com.example.lolserver.domain.{domainName}`
- 도메인 포트: `com.example.lolserver.domain.{domainName}.application.port`
- 인프라 어댑터: `com.example.lolserver.repository.{domainName}.adapter`
- 인프라 매퍼: `com.example.lolserver.repository.{domainName}.mapper`

## 설정

애플리케이션 설정은 모듈별 YAML 파일에서 가져옵니다:
- `application.yml` imports: `core.yml`, `api.yml`, `client-repository.yml`, `rabbitmq.yml`, `postgresql.yml`, `redis.yml`
- 프로파일: `local`, `prod`, `test`

Riot API 키 설정: `riot.api.key` 속성

## 테스트

### 컨트롤러 테스트
- Spring RestDocs 사용 (`module/infra/api/src/test/java/` 참조)
- RestDocs 스니펫: `build/generated-snippets`
- API 문서는 AsciiDoc으로 생성

### 도메인 서비스 테스트 (BDD 스타일)

**위치**: `module/core/lol-server-domain/src/test/java/com/example/lolserver/domain/{도메인}/application/{Service}Test.java`

**기본 템플릿**:
```java
@ExtendWith(MockitoExtension.class)
class {Service}Test {

    @Mock
    private {Port} {port};

    @InjectMocks
    private {Service} {service};

    @DisplayName("한글로 테스트 설명 작성")
    @Test
    void methodName_조건_결과() {
        // given
        given({port}.method(any())).willReturn(expected);

        // when
        var result = {service}.method(input);

        // then
        assertThat(result).isEqualTo(expected);
        then({port}).should().method(any());
    }
}
```

**테스트 작성 규칙**:
1. `// given`, `// when`, `// then` 주석으로 섹션 구분
2. Mock 설정: `BDDMockito.given()` 사용
3. 상호작용 검증: `BDDMockito.then().should()` 사용
4. 상태 검증: `AssertJ.assertThat()` 사용
5. 메서드명: 영어 (`methodName_조건_결과` 형식)
6. 테스트 설명: `@DisplayName`으로 한글 작성

**필수 import**:
```java
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
```