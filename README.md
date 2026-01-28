# LoL Server

리그 오브 레전드 전적 검색 서비스 백엔드 애플리케이션. Riot Games API를 통해 소환사 프로필, 매치 기록, 랭크 정보 등을 제공합니다.

## 기술 스택

- Java 17, Spring Boot 3.3.6, Gradle
- PostgreSQL (영속성), Redis/Redisson (캐싱), RabbitMQ (메시징)
- QueryDSL 5.1.0, MapStruct (객체 매핑), Bucket4j (Rate Limiting)
- Spring RestDocs (API 문서화), JaCoCo (코드 커버리지)

## 시작하기

### 요구사항

- Java 17+
- Docker & Docker Compose
- Riot Games API Key

### 인프라 서비스 실행

```bash
cd docker && docker-compose up -d
```

서비스 포트:
- PostgreSQL: 5432
- Redis: 6379
- RabbitMQ: 5672 (관리 UI: 15672)

### 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew build

# 로컬 실행
./gradlew bootRun -Dspring.profiles.active=local

# 테스트 실행
./gradlew test

# 클린 빌드
./gradlew clean build
```

### 환경 설정

Riot API 키는 `riot.api.key` 속성으로 설정합니다. 프로파일: `local`, `prod`, `test`

## 프로젝트 구조

헥사고날 아키텍처 (Ports & Adapters) 기반의 멀티 모듈 구조입니다.

```
module/
├── app/application/              # 진입점 (모든 모듈 의존)
├── core/
│   ├── lol-server-domain/        # 도메인 계층 + 애플리케이션 서비스 + 포트
│   └── enum/                     # 공유 enum 타입
├── infra/
│   ├── api/                      # REST 컨트롤러
│   ├── client/lol-repository/    # Riot API 클라이언트
│   ├── message/rabbitmq/         # 메시지 생산자/소비자
│   └── persistence/
│       ├── postgresql/           # JPA 엔티티, 리포지토리
│       └── redis/                # 캐싱 설정
└── support/logging/              # 로깅 유틸리티
```

### 도메인 컨텍스트

- champion: 챔피언 정보
- summoner: 소환사 프로필
- match: 매치 기록
- league: 리그 정보
- rank: 랭크 정보
- spectator: 실시간 게임 정보
- queue_type: 큐 타입

## 라이선스

이 프로젝트는 비공개 저장소입니다.
