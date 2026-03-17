# PR 리뷰 피드백 반영: Redis 갱신 마커 TTL 추가

## Context
PR #57 리뷰에서 `createSummonerRenewal()`이 TTL 없이 Redis 키를 저장하는 문제가 발견됨.
RabbitMQ 소비자 실패 시 갱신 마커가 영구 저장되어 해당 소환사의 갱신이 영원히 차단될 수 있음.

## 변경 작업

### 1. `createSummonerRenewal()`에 TTL 2분 추가
**파일**: `module/infra/persistence/redis/src/main/java/com/example/lolserver/service/SummonerCacheAdapter.java`

변경 전:
```java
stringRedisTemplate.opsForValue().set(RENEWAL_PREFIX + puuid, puuid);
```

변경 후:
```java
stringRedisTemplate.opsForValue().set(RENEWAL_PREFIX + puuid, puuid, 2, TimeUnit.MINUTES);
```

## 검증
- `./gradlew build` 실행하여 컴파일 및 테스트 통과 확인
- `SummonerCacheAdapterTest`의 `createSummonerRenewal` 관련 테스트 통과 확인
