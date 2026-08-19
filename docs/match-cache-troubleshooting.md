# 매치 캐시 트러블슈팅 기록 — 갱신 직후 새 게임이 목록에 안 나옴

> 관련: MP-XXX · PR [#155](https://github.com/padosol/lol-server/pull/155)
> 범위: `module/domain/match` (lol-server read 경로). lol-repository(write 경로)·lol-ui 는 변경 없음.

---

## 1. 문제

전적 갱신 버튼을 눌러도 방금 끝난 게임이 전적 목록에 바로 나타나지 않았다.

갱신 자체는 성공했다고 표시되는데 목록은 갱신 전 그대로였고, 새 게임을 보려면 한참 뒤에 다시 조회해야 했다. 2-tier Redis 캐시(`match:ids:v1:*` + `match:v1:*`)를 붙여 둔 목적이 바로 이 지연을 없애는 것이었으므로, 캐시가 의도대로 동작하지 않는다는 뜻이었다.

---

## 2. 원인

### 2.1 먼저 배제한 것 — 타이밍 문제가 아니다

"캐시 write 가 갱신 완료 신호보다 늦어서 못 읽는 것 아닌가"를 먼저 의심했으나, `redis-cli MONITOR` 실측 결과 순서는 정상이었다.

```
+0.000s  SETEX  summoner:renewal      (갱신 시작 마커)
+1.106s  PSETEX match:v1:*            (단건 캐시 write)
+1.112s  ZADD   match:ids:v1          (ID ZSET write)
+1.113s  DEL    summoner:renewal      (갱신 완료 신호)
```

캐시 write 가 완료 신호보다 **1ms 먼저** 끝난다. 프론트가 완료 신호를 보고 목록을 다시 조회하는 시점에는 캐시가 이미 채워져 있다.

write 측도 정상이었다. 운영 Redis 직접 확인:

```bash
kubectl exec -n <ns> deploy/redis -- redis-cli --no-raw ZREVRANGE "match:ids:v1:<PUUID>" 0 -1
1) "KR_8346406942"
```

`--no-raw` 인데도 이스케이프된 `\"` 가 없다 = 멤버가 따옴표 없는 raw 바이트로 저장돼 있다. Redisson `StringCodec` 이 정상 동작한 결과다.

즉 **깨진 곳은 lol-server 의 read 경로**였고, 결함이 두 개가 직렬로 걸려 있었다.

### 2.2 원인 1 — season 가드 때문에 캐시 경로에 진입조차 못 함

`MatchService.getMatchesBatch()` 도입부:

```java
if (season != null) {
    return loadFromDbDirect(puuid, season, queueId, pageNo);   // 캐시를 건너뛴다
}
```

프론트는 시즌 API 가 내려주는 `seasonValue`(예: `16`)를 **항상** 쿼리에 붙여 보낸다. 따라서 `season` 이 null 인 경우가 사실상 없고, `matchIdsCachePort.findIds()` 는 **호출조차 되지 않았다.**

이 가드는 실수가 아니라 안전장치였다(2.4에서 설명). 다만 그 결과 캐시 전체가 죽어 있었다.

### 2.3 원인 2 — ZSET 역직렬화 불일치 (핵심)

가드를 걷어내도 그 다음이 막혀 있었다. `MatchIdsCacheAdapter` 가 주입받던 템플릿:

```java
private final RedisTemplate<String, Object> redisTemplate;   // ← 문제
```

이 빈의 valueSerializer 는 `RedisConfig.jsonRedisSerializer()` = `GenericJackson2JsonRedisSerializer(defaultTyping=true)` 다. ZSET 멤버는 **value serializer** 로 역직렬화되므로, 이 템플릿은 멤버를 JSON 으로 파싱하려 든다.

#### 예시 데이터 — 같은 값인데 바이트가 다르다

매치 ID `KR_8346406942` 하나를 두 방식으로 저장했을 때의 실제 바이트다. 프로젝트의 실제 serializer 설정으로 실측했다.

**① lol-repository 가 실제로 쓴 것 (Redisson `StringCodec`)**

```
문자열 : KR_8346406942
길이   : 13 bytes
hex    : 4b 52 5f 38 33 34 36 34 30 36 39 34 32
```

**② 같은 값을 `GenericJackson2JsonRedisSerializer` 로 썼다면**

```
문자열 : "KR_8346406942"
길이   : 15 bytes
hex    : 22 4b 52 5f 38 33 34 36 34 30 36 39 34 32 22
         ~~                                      ~~
         0x22 = 따옴표(")                 0x22 = 따옴표(")
```

차이는 **앞뒤 따옴표 2바이트(`0x22`)** 다. JSON 에서 문자열은 반드시 `"..."` 로 감싸져야 하기 때문이다.

#### 그래서 읽을 때 터진다

**③ ①의 바이트를 JSON 직렬화기로 읽으면**

```
예외: org.springframework.data.redis.serializer.SerializationException
원인: Unrecognized token 'KR_8346406942':
      was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
```

Jackson 입장에서 따옴표 없는 `KR_8346406942` 는 JSON 문자열이 아니라 정체불명의 토큰이다. `true` 도 `null` 도 숫자도 아닌 맨 글자 덩어리라 파싱을 거부한다.

**④ 같은 바이트를 `StringRedisSerializer` 로 읽으면**

```
성공: KR_8346406942
```

**⑤ 참고 — ②의 바이트라면 JSON 직렬화기도 정상 왕복한다**

```
성공: KR_8346406942
```

JSON 직렬화기가 고장 난 게 아니라, **자기가 쓴 형식이 아닌 걸 읽으라고 시킨 것**이 문제였다.

#### 왜 이 예외가 증상으로 드러나지 않았나

```java
try {
    Set<Object> raw = redisTemplate.opsForZSet().reverseRange(key, 0, -1);  // ③ 예외 발생
    ...
} catch (Exception e) {
    log.warn("매치 ID ZSET 조회 실패 - puuid: {}, message: {}", puuid, e.getMessage());
    return Optional.empty();   // ← 예외를 "캐시 없음"으로 바꿔버린다
}
```

호출부는 이 `Optional.empty()` 를 이렇게 해석했다:

```java
if (cachedIds.isEmpty()) {
    return loadFromDbDirect(...);   // "캐시 미스구나" → DB 조회
}
```

Redis 에는 방금 쓴 매치 20건이 멀쩡히 들어 있는데, 매 요청마다 예외가 나고 → `Optional.empty()` → 서비스는 "캐시가 비었네"로 판단 → DB 폴백. **캐시 적중률이 0% 인데 `log.warn` 한 줄 말고는 아무 증상이 없었다.** 역직렬화 실패와 캐시 미스가 호출자 입장에서 구분되지 않는 구조였다.

#### 컴파일 타임에 잡히지 않은 이유

`RedisTemplate<String, Object>` 의 `Object` 는 "이 타입으로 주고받겠다"는 선언일 뿐이고 런타임에 지워진다(type erasure). 실제 바이트 해석 규칙은 전적으로 serializer 가 정한다.

```java
Set<Object> raw = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
for (Object o : raw) {
    if (o instanceof String s) { ids.add(s); }      // 컴파일 통과, 방어된 것처럼 보임
    else if (o != null) { ids.add(o.toString()); }
}
```

컴파일러는 문제를 볼 수 없다. `Object` 니까 뭐든 들어올 수 있고 `instanceof` 분기까지 있으니 안전해 보인다. 하지만 실제로는 앞줄 `reverseRange()` 에서 이미 예외가 터져 **이 루프에 도달조차 하지 않았다.**

같은 패키지의 `MatchSingleCacheAdapter` 는 처음부터 `StringRedisTemplate` 을 써서 정상 동작하고 있었다. 같은 write 측이 같은 `StringCodec` 으로 쓴 데이터를 읽는데 두 어댑터가 다른 템플릿을 쓰고 있던 것이 애초의 불일치였다.

### 2.4 season 가드를 그냥 걷어내면 안 되는 이유

캐시 ZSET 은 write 측에서 **20건으로 trim** 된다 (`MatchCacheWriteAdapter.MATCH_IDS_TRIM_KEEP`). 캐시를 "목록 전체의 소스" 로 쓰는 기존 구조에서 가드만 제거하면:

- `pageSlice(ordered, pageNo)` 가 `pageNo >= 1` 에서 빈 결과 + `hasNext=false` → **더 보기가 끊긴다**
- queueId 필터가 20건 안에서만 적용 → **모드 필터 결과 건수가 급감한다**

즉 두 결함은 **반드시 함께** 고쳐야 했다. 직렬화만 고치면 season 가드 때문에 효과가 0 이고, 가드만 걷어내면 직렬화 미스에 페이징 파손까지 더해진다.

---

## 3. 해결 방법

### 3.1 직렬화 정합 — `StringRedisTemplate` 으로 교체

```java
// before
private final RedisTemplate<String, Object> redisTemplate;      // valueSerializer = JSON

// after
private final StringRedisTemplate stringRedisTemplate;          // key/value 모두 StringRedisSerializer
```

`StringRedisTemplate` 은 `RedisTemplate<String, String>` 을 상속해 네 자리(key/value/hashKey/hashValue) serializer 를 모두 `StringRedisSerializer` 로 채워둔 하위 클래스다. 바이트 ↔ String 변환만 하므로 Redisson `StringCodec` 이 쓴 형식과 정확히 맞는다.

공용 `redisTemplate` 빈의 serializer 를 바꾸는 대안은 배제했다. 그 빈은 다른 캐시들이 `@class` FQN 타이핑에 의존하고 있어(CLAUDE.md 명시 제약) 건드리면 무관한 캐시들이 깨진다.

반환 타입이 `Set<Object>` → `Set<String>` 이 되면서 `instanceof` / `toString()` 분기도 사라졌다.

> **일반화**: RedisTemplate 은 "무엇으로 변환할지" 를 결정하는 설정 지점이고, 실제 변환은 serializer 가 한다. 외부 시스템이 쓴 키를 읽을 때는 **쓰는 쪽 코덱과 읽는 쪽 serializer 를 반드시 맞춰야** 한다.

### 3.2 캐시 역할 재정의 — "목록의 소스" 에서 "DB 첫 페이지 위의 오버레이" 로

20건 trim 이라는 제약을 받아들이고, 캐시가 목록 전체를 책임지지 않게 바꿨다.

- **DB 조회는 항상 수행.** season·queueId·페이징은 지금과 동일하게 DB 가 책임진다
- **오버레이는 `pageNo == 0` 에서만 적용.** 2페이지부터는 순수 DB 라 기존 동작이 그대로 보존된다
- **오버레이 후보 조건**: DB 첫 페이지에 없고, 그 페이지의 가장 오래된 매치(`pageFloor`)보다 최신인 것만 → 중복·순서역전 차단
- **단건 캐시에 없으면 DB 폴백 없이 버린다.** 캐시에만 있는 매치를 건지는 게 목적이므로
- **결과는 `20 + 오버레이` 를 자르지 않고 반환.** 자르면 DB 첫 페이지 꼬리가 유실된다. `hasNext` 는 DB 판단을 그대로 따른다

### 3.3 오버레이가 DB 목록 규칙과 어긋나지 않도록 맞춘 것

오버레이는 DB 페이지 위에 얹히므로, DB 쿼리의 규칙을 그대로 따라야 한다.

| 항목 | 맞춘 대상 | 이유 |
|---|---|---|
| 정렬키 | `gameEndTimestamp` | DB 가 `orderBy(matchEntity.gameEndTimestamp.desc())`. `gameCreation` 으로 비교하면 페이지 경계에서 순서 역전이 날 수 있다 |
| 게임 모드 | `CLASSIC` / `CHERRY` 만 | 같은 DB 쿼리에 이 조건이 있는데 캐시 ZSET 은 모드 무관하게 써진다. 제한이 없으면 갱신 직후 ARAM 이 "전체" 목록 최상단에만 튀어나온다 |
| 시즌 판별 | `gameVersion` major | 캐시 JSON 에 season 필드가 없다. lol-repository `MatchEntity` 가 `gameVersion.split("\\.")[0]` 을 `season` 컬럼에 넣으므로 규칙이 동일하다 (`"16.16.804.9184"` → `16`) |

세 필드 모두 캐시 JSON 에 채워져 있음을 lol-repository `MatchCacheViewMapper.toGameInfo()` 에서 확인했다. 시즌 판별이 불가능한 경우(gameVersion 이 비었거나 파싱 실패)는 시즌 필터 결과를 오염시키지 않도록 오버레이에서 제외한다.

### 3.4 손대지 않은 것

- **lol-repository** — write 경로, 마커 삭제 시점 모두 현행 유지
- **lol-ui** — 변경 불필요
- `AsyncMatchSaver` 비동기 적재 — 동기로 바꾸면 큐 처리량만 떨어진다
- `renewal-status` 상태 머신 — "마커 없음 = SUCCESS" 결함은 실재하나 별건, 후속 과제

---

## 4. 결과

### 4.1 검증

- `./gradlew build` 전체 통과 (checkstyle 포함)
- `MatchServiceTest` 16건 / `MatchIdsCacheAdapterTest` 3건 통과

추가된 주요 케이스:

- 캐시에만 있는 최신 매치를 DB 첫 페이지 위에 얹고 DB 꼬리는 자르지 않는다
- 2페이지부터는 캐시를 보지 않고 DB 결과를 그대로 반환한다
- ZSET miss / 빈 ZSET 이어도 DB 결과를 그대로 반환한다
- DB 첫 페이지 최하단보다 오래된 캐시 매치는 오버레이하지 않는다
- 단건 캐시에 없는 후보는 DB 폴백 없이 버린다
- season 필터가 DB 에 전달되고 오버레이는 `gameVersion` major 로 걸러진다
- queueId 필터 / 게임 모드 제한이 오버레이 후보에도 적용된다
- DB 첫 페이지가 비어 있어도 캐시 매치를 반환한다

### 4.2 동작 변화

| | 수정 전 | 수정 후 |
|---|---|---|
| ZSET 읽기 | 매 요청 역직렬화 실패 → 캐시 미스로 위장 | 정상 |
| 캐시 진입 | `season` 이 붙어 있어 진입 자체를 안 함 | 항상 평가 |
| 캐시 역할 | 목록 전체의 소스 (20건 trim 에 걸림) | DB 첫 페이지 위 오버레이 |
| 2페이지 이후 | 캐시 경로에서 빈 결과 위험 | 순수 DB, 기존 동작 보존 |
| 갱신 직후 새 게임 | DB 적재 완료까지 안 보임 | 캐시에서 즉시 노출 |

### 4.3 남은 것

- **실환경 실측 미완료** — "갱신 후 2초 내 새 게임이 목록 최상단에 노출" 은 Postgres/Redis/RabbitMQ 실행 + 실제 갱신 플로우가 필요해 단위 테스트 범위 밖이다. 배포 후 확인 필요
- **`MatchPersistencePort.findMatchesByIds` 가 프로덕션 미사용이 됨** — 오버레이가 DB 폴백을 하지 않기 때문. `findRecentMatchIds` 도 이미 미사용 상태라 기존 조건과 같고, 포트 정리는 이번 범위를 넘어 손대지 않았다

---

## 5. 교훈

1. **캐시 실패를 캐시 미스와 같은 값으로 뭉개지 말 것.** `catch → Optional.empty()` 는 장애를 정상 동작으로 위장시킨다. 최소한 메트릭으로 실패율이 드러나야 한다.
2. **외부 시스템이 쓴 Redis 키를 읽을 때는 코덱/serializer 를 먼저 맞춰볼 것.** 제네릭 타입은 런타임에 아무것도 보장하지 않는다.
3. **같은 데이터를 읽는 어댑터끼리 템플릿이 다르면 그 자체가 신호다.** `MatchSingleCacheAdapter` 가 정답 레퍼런스로 옆에 있었다.
4. **안전장치처럼 보이는 가드는 지우기 전에 왜 있었는지 확인할 것.** season 가드는 20건 trim 으로 인한 페이징 파손을 막고 있었다. 원인을 제거(캐시 역할 재정의)해야 가드를 걷어낼 수 있다.

---

## See Also

- `module/domain/match/src/main/java/com/example/lolserver/match/application/MatchService.java` — 오버레이 로직
- `module/domain/match/src/main/java/com/example/lolserver/match/adapter/out/cache/` — 두 캐시 어댑터
- `module/common/src/main/java/com/example/lolserver/common/config/RedisConfig.java` — 공용 RedisTemplate serializer 설정
- lol-repository `module/infra/redis/.../MatchCacheWriteAdapter.java` — write 경로 (StringCodec, 20건 trim, 3분 TTL)
