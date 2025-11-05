# LoL Match History Service

리그 오브 레전드 게임의 전적 검색 서비스를 제공하는 Spring Boot 기반의 백엔드 애플리케이션입니다.

## 기술 스택

- Java
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Redis
- RabbitMQ
- QueryDSL
- Bucket4j

## 주요 기능

### 1. 소환사 정보 조회
- 소환사 기본 정보 검색
- 게임 전적 기록 조회
- 리그 정보 및 랭크 조회

### 2. 실시간 데이터 처리
- RabbitMQ를 활용한 비동기 데이터 처리
- Redis를 이용한 캐싱 시스템
- 실시간 전적 업데이트

### 3. Rate Limiting
- Bucket4j를 활용한 API 요청 제한
- Redis 기반의 분산 Rate Limiting
- Riot API 호출 최적화

## 시스템 아키텍처

### 데이터베이스
- PostgreSQL: 주 데이터베이스
- Redis: 캐싱 및 Rate Limiting
- 
### 메시지 큐
- RabbitMQ: 비동기 메시지 처리

### API 통신
- Riot Games API 연동
- WebClient를 활용한 비동기 HTTP 통신
- 커스텀 예외 처리

## 설치 및 실행

### 필수 요구사항
- Java 17 이상
- PostgreSQL
- Redis
- RabbitMQ

### 환경 설정

1. 데이터베이스 설정
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: [username]
    password: [password]
```

2. Redis 설정
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

3. RabbitMQ 설정
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

4. Riot API 키 설정

```yaml
riot.api.key: [your-api-key]
```

### 실행 방법

1. 저장소 클론
```bash
git clone [repository-url]
```

2. 프로젝트 빌드
```bash
./gradlew build
```

3. 애플리케이션 실행
```bash
java -jar build/libs/lol-server-[version].jar
```

## API 엔드포인트

### 소환사 정보
- GET /api/summoners/{summonerName} - 소환사 정보 조회
- GET /api/summoners/{summonerId}/leagues - 소환사의 리그 정보 조회
- GET /api/summoners/{summonerId}/matches - 소환사의 매치 히스토리 조회

## 성능 최적화

1. 데이터베이스
- JPA 배치 처리 최적화
- QueryDSL을 활용한 동적 쿼리 최적화

2. 캐싱 전략
- Redis를 활용한 데이터 캐싱
- 실시간 데이터 동기화

3. API 요청 관리
- Bucket4j를 통한 Rate Limiting
- 분산 환경에서의 요청 제어

## 모니터링 및 로깅

- Spring Actuator를 통한 헬스 체크
- 상세한 로깅 설정
- 에러 추적 및 모니터링
