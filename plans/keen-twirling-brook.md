# DTO 명칭 통일 리팩토링 계획

## Context

도메인 계층에서 조회용 DTO 네이밍이 `*Response`와 `*ReadModel`로 혼재되어 있음.
통일 규칙:
- **API**: `*Request`, `*Response`
- **Domain**: 조회용 `*ReadModel` (`application/model/`), 도메인은 그대로
- **Persistence**: `*Entity`, `*DTO`

ReadModel은 API 계층에서 그대로 반환한다.

## 변경 범위

### 도메인 계층: `*Response` → `*ReadModel` + 패키지 `dto/` → `model/` (15개 파일)

#### 1. ChampionStats (7개)

| 현재 | 변경 후 |
|------|---------|
| `ChampionStatsResponse` | `ChampionStatsReadModel` |
| `ChampionPositionStatsResponse` | `ChampionPositionStatsReadModel` |
| `ChampionMatchupResponse` | `ChampionMatchupReadModel` |
| `ChampionItemBuildResponse` | `ChampionItemBuildReadModel` |
| `ChampionRuneBuildResponse` | `ChampionRuneBuildReadModel` |
| `ChampionSkillBuildResponse` | `ChampionSkillBuildReadModel` |
| `ChampionWinRateResponse` | `ChampionWinRateReadModel` |

**영향받는 파일:**
- `domain/championstats/application/ChampionStatsService.java`
- `domain/championstats/application/port/out/ChampionStatsQueryPort.java`
- `controller/championstats/ChampionStatsController.java`
- `persistence/clickhouse/adapter/ChampionStatsClickHouseAdapter.java`
- 테스트: `ChampionStatsServiceTest`, `ChampionStatsControllerTest`, `ChampionStatsClickHouseAdapterTest`

#### 2. Match (3개)

| 현재 | 변경 후 |
|------|---------|
| `GameResponse` | `GameReadModel` |
| `MatchResponse` | `MatchReadModel` |
| `DailyGameCountResponse` | `DailyGameCountReadModel` |

**영향받는 파일:**
- `domain/match/application/MatchService.java`
- `domain/match/application/port/out/MatchPersistencePort.java`
- `repository/match/adapter/MatchPersistenceAdapter.java`
- `controller/match/MatchController.java`
- 테스트: `MatchServiceTest`, `MatchControllerTest`, `MatchPersistenceAdapterTest`

> 참고: `GameResponse`, `MatchResponse`는 `@Getter/@Setter` 클래스. 이번에는 이름만 변경하고, record 전환은 별도 작업으로 진행.

#### 3. Summoner (3개)

| 현재 | 변경 후 |
|------|---------|
| `SummonerResponse` | `SummonerReadModel` |
| `SummonerAutoResponse` | `SummonerAutoReadModel` |
| `SummonerRenewalInfoResponse` | `SummonerRenewalInfoReadModel` |

**영향받는 파일:**
- `domain/summoner/application/SummonerService.java` — `SummonerResponse.of()`, `SummonerResponse.builder()` 등 팩토리/빌더 호출부 변경 필요
- `controller/summoner/SummonerController.java`
- `controller/admin/AdminSummonerController.java`
- 테스트: `SummonerServiceTest`, `SummonerControllerTest`, `AdminSummonerControllerTest`

> 참고: `SummonerCommand`가 `application/dto/`에 남게 됨 (현재 미사용, 별도 정리 대상)

#### 4. Rank (1개)

| 현재 | 변경 후 |
|------|---------|
| `RankResponse` | `RankReadModel` |

**영향받는 파일:**
- `domain/rank/application/RankService.java`
- `domain/rank/application/port/in/RankUseCase.java`
- `controller/rank/RankController.java`
- 테스트: `RankServiceTest`, `RankControllerTest`

#### 5. Season (1개)

| 현재 | 변경 후 |
|------|---------|
| `SeasonResponse` | `SeasonReadModel` |

**영향받는 파일:**
- `domain/season/application/SeasonService.java`
- `domain/season/application/port/out/SeasonPersistencePort.java`
- `repository/season/mapper/SeasonMapper.java` — `toResponse()` → `toReadModel()` 메서드명 변경
- `repository/season/adapter/SeasonPersistenceAdapter.java`
- `controller/season/SeasonController.java`
- 테스트: `SeasonControllerTest`

### 영속성 계층 (1개)

| 현재 | 변경 후 |
|------|---------|
| `LinePosition` | `LinePositionDTO` |

**영향받는 파일:**
- `repository/match/matchsummoner/dsl/MatchSummonerRepositoryCustom.java`
- `repository/match/matchsummoner/dsl/impl/MatchSummonerRepositoryCustomImpl.java`
- 테스트: `MatchSummonerRepositoryCustomImplTest`

### API 계층 정리 (1개 삭제)

- `controller/summoner/response/SummonerAutoResponse.java` — 빈 record, 미사용 → **삭제**

### 문서 업데이트

- `CLAUDE.md` — Read Model 패턴 표, 클래스 작성요령 표 업데이트

## 변경하지 않는 것

- 이미 `*ReadModel` 패턴 사용 중: Spectator, PatchNote, Version, TierCutoff
- API 계층 Response 클래스: `LeagueResponse`, `ChampionRotateResponse`, `SummonerRenewalResponse`, `QueueInfoResponse` 등
- Client 계층 VO 네이밍
- 클래스 mutability (class → record 전환은 별도)

## 구현 순서

의존성이 없는 도메인들은 **독립적으로 처리 가능**. 각 도메인 내에서는 다음 순서:

1. `application/model/` 디렉토리 생성
2. 새 ReadModel 파일 생성 (패키지 + 클래스명 변경)
3. 도메인 내부 참조 업데이트 (서비스, 포트)
4. 인프라 참조 업데이트 (어댑터, 매퍼, 컨트롤러)
5. 테스트 참조 업데이트
6. 기존 `application/dto/` 파일 삭제
7. 빈 `application/dto/` 디렉토리 삭제 (다른 파일이 없는 경우)

**도메인 처리 순서** (영향 범위가 작은 것부터):
1. Season (1개 파일, 7개 참조)
2. Rank (1개 파일, 6개 참조)
3. Summoner (3개 파일, 6-12개 참조)
4. Match (3개 파일, 10개 참조)
5. ChampionStats (7개 파일, 13개 참조)
6. Persistence: LinePosition → LinePositionDTO
7. API 정리: 빈 SummonerAutoResponse 삭제
8. CLAUDE.md 업데이트

## 주의사항

- **ChampionStatsResponse 이름 충돌**: 도메인의 `ChampionStatsResponse`와 API 계층의 `controller/match/response/ChampionStatsResponse`는 **다른 클래스**. 도메인만 `ChampionStatsReadModel`로 변경하면 충돌 해소됨.
- **SummonerResponse.builder()**: `SummonerReadModel.builder()`로 변경 시 빌더 클래스명도 자동 변경됨 (Lombok `@Builder`)
- **MapStruct SeasonMapper**: `toResponse()` → `toReadModel()`로 메서드명도 변경 필요
- **MatchResponse 내부 참조**: `MatchResponse`가 `GameResponse`를 참조하므로 두 파일 동시에 변경해야 함

## 검증

```bash
# 1. 빌드 확인
./gradlew clean build

# 2. 테스트 실행
./gradlew test

# 3. import 정리 확인 - 기존 dto 패키지 참조가 남아있지 않은지
grep -r "application.dto.*Response" module/ --include="*.java"

# 4. 삭제된 파일 참조 확인
grep -r "GameResponse\|MatchResponse\|SummonerResponse\|RankResponse\|SeasonResponse\|ChampionStatsResponse\|DailyGameCountResponse\|SummonerAutoResponse\|SummonerRenewalInfoResponse" module/core/ --include="*.java"
```
