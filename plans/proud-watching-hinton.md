# 사용하지 않는 함수 삭제

## Context
코드베이스 전체를 탐색하여 정의만 되어 있고 어디서도 호출되지 않는 메서드/클래스를 식별했습니다. 불필요한 코드를 제거하여 유지보수성을 높이기 위한 작업입니다.

## 삭제 대상 (8건)

### 1. `Summoner.clickRenewal()` — 미사용 도메인 메서드
- **파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/summoner/domain/Summoner.java:30-32`
- `lastRiotCallDate`를 갱신하는 메서드지만 어디서도 호출되지 않음

### 2. `SummonerEntity.clickRenewal()` — 미사용 엔티티 메서드
- **파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/summoner/entity/SummonerEntity.java:61-63`
- 도메인과 동일한 미사용 메서드

### 3. `DuoPost.markExpired()` — 미사용 도메인 메서드
- **파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/duo/domain/DuoPost.java:101-104`
- `markMatched()`, `markDeleted()`는 사용되지만 `markExpired()`는 미사용

### 4. `DuoRequest.isRequester()` — 미사용 boolean 쿼리 메서드
- **파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/duo/domain/DuoRequest.java:64-66`
- `validateRequester()`가 대신 사용됨

### 5. `TierInfo.isAvailable()` — 미사용 VO 메서드
- **파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/duo/domain/vo/TierInfo.java:10-12`
- `tier != null` 체크이지만 어디서도 호출되지 않음

### 6. `Platform.getValueOfName()` — 미사용 정적 메서드
- **파일**: `module/core/enum/src/main/java/com/example/lolserver/Platform.java:49-56`
- `Platform.valueOfName()`이 사용되며, 이 메서드는 미사용

### 7. `DuoPostMapper.toDomainList()` — 미사용 매퍼 메서드
- **파일**: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/duo/mapper/DuoPostMapper.java:27`
- 어댑터에서 `toDomain()`만 사용, `toDomainList()`는 미사용

### 8. `Queue` enum — 빈 미사용 클래스
- **파일**: `module/core/enum/src/main/java/com/example/lolserver/Queue.java`
- 내용 없는 빈 enum, 어디서도 import/참조되지 않음 (`QueueType`이 실제 사용됨)

## 제외 대상

- `WebConfig` (빈 `@Configuration` 클래스): Spring이 스캔하는 설정 클래스이므로 향후 확장 가능성을 고려하여 유지

## 검증 방법
- `./gradlew build` 실행하여 컴파일 오류 없는지 확인
- `./gradlew test` 실행하여 기존 테스트 통과 확인
