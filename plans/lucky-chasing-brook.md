# 듀오 찾기 기능 수정 계획

## Context

듀오 찾기 기능의 UX 개선을 위해 4가지 변경을 진행한다:
1. **보조라인 제거, 희망라인 추가**: `secondaryLane`(내 보조 포지션) → `desiredLane`(듀오 파트너에게 원하는 포지션)으로 의미 변경
2. **만료기간 1시간으로 단축**: 24시간 → 1시간
3. **모스트 챔피언 3개 추가**: 솔로랭크 기준 가장 많이 플레이한 챔피언 3개 스냅샷
4. **최근 20게임 승패 및 플레이챔피언 추가**: 솔로랭크 최근 20게임의 승/패 수, 플레이한 챔피언 목록 스냅샷

DuoPost와 DuoRequest 모두 동일하게 적용한다.

---

## 변경 대상 파일 목록

### Domain Layer (core/lol-server-domain)

| 파일 | 변경 내용 |
|------|----------|
| `domain/duo/domain/DuoPost.java` | `secondaryLane` → `desiredLane`, 만료 24h→1h, 새 필드 추가 |
| `domain/duo/domain/DuoRequest.java` | `secondaryLane` → `desiredLane`, 새 필드 추가 |
| `domain/duo/domain/vo/MostChampion.java` | **신규** - 모스트 챔피언 VO (record) |
| `domain/duo/domain/vo/RecentGameSummary.java` | **신규** - 최근 승패 + 플레이챔피언 VO (record) |
| `domain/duo/application/DuoService.java` | 챔피언/매치 데이터 조회 로직 추가 |
| `domain/duo/application/DuoRequestService.java` | 챔피언/매치 데이터 조회 로직 추가 |
| `domain/duo/application/RiotAccountResolver.java` | 모스트챔피언, 최근게임 조회 메서드 추가 |
| `domain/duo/application/command/CreateDuoPostCommand.java` | `secondaryLane` → `desiredLane` |
| `domain/duo/application/command/UpdateDuoPostCommand.java` | `secondaryLane` → `desiredLane` |
| `domain/duo/application/command/CreateDuoRequestCommand.java` | `secondaryLane` → `desiredLane` |
| `domain/duo/application/model/DuoPostReadModel.java` | `secondaryLane` → `desiredLane`, 새 필드 추가 |
| `domain/duo/application/model/DuoPostListReadModel.java` | `secondaryLane` → `desiredLane`, 새 필드 추가 |
| `domain/duo/application/model/DuoPostDetailReadModel.java` | `secondaryLane` → `desiredLane`, 새 필드 추가 |
| `domain/duo/application/model/DuoRequestReadModel.java` | `secondaryLane` → `desiredLane`, 새 필드 추가 |

### Persistence Layer (infra/persistence/postgresql)

| 파일 | 변경 내용 |
|------|----------|
| `repository/duo/entity/DuoPostEntity.java` | `secondaryLane` → `desiredLane`, JSON 컬럼 추가 |
| `repository/duo/entity/DuoRequestEntity.java` | `secondaryLane` → `desiredLane`, JSON 컬럼 추가 |
| `repository/duo/mapper/DuoPostMapper.java` | 필드명 매핑 변경 |
| `repository/duo/mapper/DuoRequestMapper.java` | 필드명 매핑 변경 |
| `repository/duo/dto/DuoPostListDTO.java` | `secondaryLane` → `desiredLane` |
| `repository/duo/dsl/DuoPostRepositoryCustomImpl.java` | QueryDSL 프로젝션 필드명 변경 |
| `repository/duo/adapter/DuoPostPersistenceAdapter.java` | ReadModel 빌더 필드명 변경 |
| `repository/duo/adapter/DuoRequestPersistenceAdapter.java` | ReadModel 빌더 필드명 변경 |
| `repository/duo/converter/MostChampionListConverter.java` | **신규** - JPA AttributeConverter (JSON ↔ List<MostChampion>) |
| `repository/duo/converter/RecentGameSummaryConverter.java` | **신규** - JPA AttributeConverter (JSON ↔ RecentGameSummary) |

### API Layer (infra/api)

| 파일 | 변경 내용 |
|------|----------|
| `controller/duo/request/CreateDuoPostRequest.java` | `secondaryLane` → `desiredLane` |
| `controller/duo/request/UpdateDuoPostRequest.java` | `secondaryLane` → `desiredLane` |
| `controller/duo/request/CreateDuoRequestRequest.java` | `secondaryLane` → `desiredLane` |
| `controller/duo/response/DuoPostResponse.java` | `secondaryLane` → `desiredLane`, 새 필드 추가 |
| `controller/duo/response/DuoPostListResponse.java` | `secondaryLane` → `desiredLane`, 새 필드 추가 |
| `controller/duo/response/DuoPostDetailResponse.java` | `secondaryLane` → `desiredLane`, 새 필드 추가 |
| `controller/duo/response/DuoRequestResponse.java` | `secondaryLane` → `desiredLane`, 새 필드 추가 |

### 테스트

| 파일 | 변경 내용 |
|------|----------|
| `DuoServiceTest.java` | 필드명 변경 + 새 필드 검증 |
| `DuoRequestServiceTest.java` | 필드명 변경 + 새 필드 검증 |
| `RiotAccountResolverTest.java` | 새 메서드 테스트 추가 |
| `DuoPostMapperTest.java` | 필드명 변경 + JSON 변환 검증 |
| `DuoRequestMapperTest.java` | 필드명 변경 + JSON 변환 검증 |
| `DuoPostControllerTest.java` (RestDocs) | 필드명 변경 + 새 필드 문서화 |
| `DuoRequestControllerTest.java` (RestDocs) | 필드명 변경 + 새 필드 문서화 |

---

## 구현 단계

### Step 1: 새 도메인 VO 생성

**`MostChampion.java`** (신규 - `domain/duo/domain/vo/`)
```java
public record MostChampion(
    int championId,
    String championName,
    long playCount,
    long wins,
    long losses
) {}
```

**`RecentGameSummary.java`** (신규 - `domain/duo/domain/vo/`)
```java
public record RecentGameSummary(
    int wins,
    int losses,
    List<PlayedChampion> playedChampions
) {
    public record PlayedChampion(int championId, String championName) {}
}
```

### Step 2: 도메인 객체 수정

**`DuoPost.java`** 변경:
- `secondaryLane` → `desiredLane` (필드, 생성자, updateContent 메서드)
- `List<MostChampion> mostChampions` 필드 추가
- `RecentGameSummary recentGameSummary` 필드 추가
- `create()` 파라미터에 `List<MostChampion>`, `RecentGameSummary` 추가
- `expiresAt` = `now.plusHours(1)` (24 → 1)

**`DuoRequest.java`** 동일 패턴 적용:
- `secondaryLane` → `desiredLane`
- `mostChampions`, `recentGameSummary` 추가
- `create()` 파라미터 확장

### Step 3: RiotAccountResolver 확장

`RiotAccountResolver.java`에 새 메서드 추가:
- `lookupMostChampions(String puuid)` → `List<MostChampion>` (상위 3개)
  - 기존 `MatchPersistencePort.getRankChampions(puuid, season, queueId)` 활용
  - MSChampion → MostChampion 변환, limit 3
- `lookupRecentGameSummary(String puuid)` → `RecentGameSummary`
  - 기존 `MatchPersistencePort.getMatches(puuid, queueId, paginationRequest)` 활용 (size=20)
  - 참가자 데이터에서 해당 puuid의 win/loss, championId/Name 추출

**의존성 추가**: `MatchPersistencePort` 주입

### Step 4: Command, ReadModel 수정

모든 Command/ReadModel에서 `secondaryLane` → `desiredLane` 일괄 변경.

ReadModel에 새 필드 추가:
- `DuoPostReadModel`: `List<MostChampion> mostChampions`, `RecentGameSummary recentGameSummary`
- `DuoPostListReadModel`: 동일 추가
- `DuoPostDetailReadModel`: 동일 추가
- `DuoRequestReadModel`: 동일 추가

### Step 5: Application Service 수정

**`DuoService.createDuoPost()`**:
```java
List<MostChampion> mostChampions = riotAccountResolver.lookupMostChampions(puuid);
RecentGameSummary recentGameSummary = riotAccountResolver.lookupRecentGameSummary(puuid);
// DuoPost.create()에 전달
```

**`DuoRequestService.createDuoRequest()`**: 동일 패턴

### Step 6: Persistence Layer 수정

**JPA AttributeConverter 신규 생성**:
- `MostChampionListConverter`: `List<MostChampion>` ↔ JSON String (Jackson ObjectMapper 사용)
- `RecentGameSummaryConverter`: `RecentGameSummary` ↔ JSON String

**Entity 수정** (`DuoPostEntity`, `DuoRequestEntity`):
- `secondary_lane` 컬럼 → `desired_lane` 컬럼 (DB 마이그레이션 필요)
- `most_champions` 컬럼 추가 (TEXT, JSON)
- `recent_game_summary` 컬럼 추가 (TEXT, JSON)
- `@Convert(converter = MostChampionListConverter.class)` 적용

**MapStruct Mapper 수정**:
- `secondaryLane` → `desiredLane` 매핑 변경
- JSON 컬럼 필드는 MapStruct가 자동 매핑 (동일 타입)

**QueryDSL** (`DuoPostRepositoryCustomImpl`):
- `duoPostEntity.secondaryLane` → `duoPostEntity.desiredLane`
- `DuoPostListDTO` 프로젝션 필드명 변경

**Adapter** (`DuoPostPersistenceAdapter`, `DuoRequestPersistenceAdapter`):
- ReadModel 빌더에서 `secondaryLane` → `desiredLane`
- 새 필드 매핑 추가

### Step 7: API Layer 수정

**Request DTOs**: `secondaryLane` → `desiredLane`

**Response DTOs**: `secondaryLane` → `desiredLane` + 새 필드 추가
- `mostChampions`: `List<MostChampionResponse>` (내부 record)
- `recentGameSummary`: `RecentGameSummaryResponse` (내부 record)

### Step 8: 테스트 수정

- 모든 테스트에서 `secondaryLane` → `desiredLane` 변경
- 새 필드(mostChampions, recentGameSummary) 검증 추가
- `RiotAccountResolverTest`에 `lookupMostChampions`, `lookupRecentGameSummary` 테스트 추가
- MapperTest에 JSON 변환 검증 추가
- RestDocs 테스트에 새 필드 문서화

### Step 9: DB 마이그레이션

SQL 마이그레이션 (duo_post, duo_request 테이블):
```sql
ALTER TABLE duo_post RENAME COLUMN secondary_lane TO desired_lane;
ALTER TABLE duo_post ADD COLUMN most_champions TEXT;
ALTER TABLE duo_post ADD COLUMN recent_game_summary TEXT;

ALTER TABLE duo_request RENAME COLUMN secondary_lane TO desired_lane;
ALTER TABLE duo_request ADD COLUMN most_champions TEXT;
ALTER TABLE duo_request ADD COLUMN recent_game_summary TEXT;
```

---

## 검증 방법

1. `./gradlew test` - 전체 테스트 통과 확인
2. `./gradlew :module:infra:api:asciidoctor` - RestDocs 문서 재생성
3. `./gradlew build` - 전체 빌드 성공 확인
4. API 응답에서 `secondaryLane` 대신 `desiredLane` 반환 확인
5. 게시글 생성 시 `expiresAt`이 1시간 후로 설정되는지 확인
6. 게시글 생성 시 `mostChampions` (3개), `recentGameSummary` (20게임 기준) 데이터 포함 확인
