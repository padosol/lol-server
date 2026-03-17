# SummonerResponse 날짜 포맷을 yyyy-MM-dd HH:mm:ss로 변경

## Context
이전 작업에서 `SummonerResponse`의 날짜 포맷을 `yyyy-MM-dd`로 변경했으나, 시/분/초까지 출력이 필요하다.

## 수정 파일

### `SummonerResponse.java`
- 경로: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/summoner/application/dto/SummonerResponse.java`
- `DATE_FORMATTER` 패턴을 `"yyyy-MM-dd"` → `"yyyy-MM-dd HH:mm:ss"`로 변경 (18번 줄)

```java
private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
```

## 검증
- `./gradlew :module:core:lol-server-domain:compileJava :module:core:lol-server-domain:test` 성공 확인
