# Core 모듈 DDD 리팩토링 계획

## Context

DDD 분석 결과, core 모듈(`module/core/lol-server-domain`)에서 6가지 아키텍처 위반이 확인되었습니다. 가장 심각한 문제는 Spring Data 의존성이 도메인 포트/서비스까지 침투한 것이며, build.gradle에서 이미 "TEMPORARY"로 인지하고 있습니다. 이 계획은 **ROI(영향/노력 비율) 순**으로 단계별 독립 배포 가능한 리팩토링을 설계합니다.

---

## Phase 0: 기존 `Page<T>` → `SliceResult<T>` 리네이밍 (선행 작업)

**목표**: 기존 `support/Page.java`를 `SliceResult`로 이름 변경하여 Spring Data `Page`와의 혼동 제거 및 의미 명확화

**이유**: `Page`라는 이름이 Spring Data `Page`와 충돌하고, 실제 시맨틱은 slice(offset 없는 다음 페이지 존재 여부)임

### 변경 내용

**0a. 파일 리네이밍**
- `support/Page.java` → `support/SliceResult.java` (클래스명 `Page<T>` → `SliceResult<T>`)

**0b. 전체 참조 업데이트 (15개 파일)**

| 파일 | 변경 내용 |
|------|-----------|
| `domain/community/application/PostService.java` | import 및 타입 참조 변경 |
| `domain/community/application/port/in/PostQueryUseCase.java` | 반환 타입 변경 |
| `domain/community/application/port/out/PostPersistencePort.java` | 반환 타입 변경 |
| `domain/match/application/MatchService.java` | import 및 타입 참조 변경 |
| `domain/match/application/port/out/MatchPersistencePort.java` | 반환 타입 변경 |
| `repository/match/adapter/MatchPersistenceAdapter.java` | import 및 타입 참조 변경 |
| `repository/community/adapter/PostPersistenceAdapter.java` | import 및 타입 참조 변경 |
| `controller/match/MatchController.java` | import 및 타입 참조 변경 |
| `controller/community/CommunityPostController.java` | import 및 타입 참조 변경 |
| `controller/support/response/SliceResponse.java` | `Page` → `SliceResult` import/파라미터 변경 |
| `PostServiceTest.java` | import 및 타입 참조 변경 |
| `MatchServiceTest.java` | import 및 타입 참조 변경 |
| `MatchPersistenceAdapterTest.java` | import 및 타입 참조 변경 |
| `MatchControllerTest.java` | import 및 타입 참조 변경 |
| `CommunityPostControllerTest.java` | import 및 타입 참조 변경 |

**검증**: `grep -r "import com.example.lolserver.support.Page" .` → 0건, `./gradlew test` 통과

**파일 수**: 1 리네이밍 + 14 참조 수정 = **~15개**

---

## Phase 1: Rank Context Spring Data 제거 (Critical)

**목표**: `RankUseCase`, `RankPersistencePort`에서 `org.springframework.data.domain.Page` 제거

**이유**: UseCase(Inbound Port)에 Spring Data 타입이 노출되어 도메인 계층의 인프라 독립성이 완전히 깨짐

### 변경 내용

**1a. 도메인 수준 `PageResult<T>` 생성**

Rank는 total count 시맨틱이 필요하므로 `SliceResult`와 별도 record 추가:

```java
// support/PageResult.java
public record PageResult<T>(
    List<T> content, int page, int size,
    long totalElements, int totalPages,
    boolean isFirst, boolean isLast
) { }
```

**1b. Core 파일 수정 (3개)**
- `domain/rank/application/port/in/RankUseCase.java` — `Page<RankReadModel>` → `PageResult<RankReadModel>`
- `domain/rank/application/port/out/RankPersistencePort.java` — `Page<Rank>` → `PageResult<Rank>`
- `domain/rank/application/RankService.java` — 반환 타입 변경, 매핑 로직 조정

**1c. Infra 파일 수정 (2개)**
- `repository/rank/adapter/RankPersistenceAdapter.java` — Spring Data `Page` → `PageResult` 변환 (어댑터 경계에서)
- `controller/rank/RankController.java` 또는 `controller/support/response/PageResponse.java` — `PageResult` 수용

**1d. 테스트 수정 (2개)**
- `RankServiceTest.java` — `PageImpl` → `PageResult`
- `RankControllerTest.java` — 동일

**검증**: `grep -r "org.springframework.data" module/core/.../rank/` → 0건

**파일 수**: 1 신규 + 5 수정 + 2 테스트 = **~8개**

---

## Phase 2: Match Context Spring Data 제거 (Critical)

**목표**: `MatchPersistencePort`의 `Pageable` 파라미터, `MatchService`의 `PageRequest`/`Sort` 직접 생성 제거

**이유**: Phase 1과 합쳐 `spring-data-commons` 의존성을 build.gradle에서 완전 삭제

### 변경 내용

**2a. 도메인 수준 `PaginationRequest` 생성**

```java
// support/PaginationRequest.java
public record PaginationRequest(
    int page, int size, String sortBy, SortDirection direction
) {
    public enum SortDirection { ASC, DESC }
}
```

**2b. Core 파일 수정 (2개)**
- `domain/match/application/port/out/MatchPersistencePort.java` — `Pageable` → `PaginationRequest`
- `domain/match/application/MatchService.java` — `PageRequest.of()`/`Sort.by()` → `new PaginationRequest(...)`, 모든 `org.springframework.data` import 제거

**2c. Infra 파일 수정 (1개)**
- `repository/match/adapter/MatchPersistenceAdapter.java` — `PaginationRequest` → Spring Data `PageRequest` 변환 (어댑터 경계)

**2d. 테스트 수정 (1개)**
- `MatchServiceTest.java` — `any(Pageable.class)` → `any(PaginationRequest.class)`

**2e. build.gradle에서 spring-data-commons 삭제**
```diff
- implementation 'org.springframework.data:spring-data-commons'
```

**검증**: 
- `grep -r "org.springframework.data" module/core/lol-server-domain/src/main/` → **0건**
- `./gradlew :module:core:lol-server-domain:compileJava` 성공
- `./gradlew test` 전체 통과

**파일 수**: 1 신규 + 3 수정 + 1 테스트 + build.gradle = **~6개**

---

## Phase 3: Summoner/LeagueSummoner `@Setter` 제거 (High)

**목표**: CLAUDE.md 컨벤션 위반("도메인 객체에 @Setter 사용 금지") 해소, Aggregate 불변식 보호

**이유**: `@Setter`로 모든 필드가 노출되어 아무 코드에서나 상태 변경 가능 → 불변식 우회

### 변경 내용

**3a. 도메인 객체 수정 (2개)**
- `domain/summoner/domain/Summoner.java`:
  ```diff
  - @Setter
  + @Builder(toBuilder = true)
  + @AllArgsConstructor(access = AccessLevel.PRIVATE)
  ```
  - 기존 `clickRenewal()`, `isRevision()` 유지
  - 외부에서 setter로 호출하던 필드 갱신을 의미 있는 도메인 메서드로 대체 (예: `updateProfile(...)`)

- `domain/summoner/domain/LeagueSummoner.java`:
  ```diff
  - @Setter
  + @Builder
  + @AllArgsConstructor(access = AccessLevel.PRIVATE)
  ```

**3b. MapStruct 매퍼 확인/수정 (~2개)**
- `repository/summoner/mapper/SummonerMapper.java` — MapStruct 1.5.5는 `@Builder` 자동 감지. 매핑 전략 확인
- `client/lol-repository/.../SummonerClientMapper.java` — 동일

**3c. 테스트 수정 (~4개)**
- `SummonerServiceTest.java` — `new Summoner()` → `Summoner.builder()...build()`
- `SummonerClientAdapterTest.java` — 동일
- `SummonerMapperTest.java`, `SummonerClientMapperTest.java` — 매퍼 테스트 검증

**검증**:
- `grep -r "@Setter" module/core/.../domain/summoner/` → 0건
- 매퍼 테스트 통과
- `./gradlew test` 전체 통과

**파일 수**: 2 도메인 + 2 매퍼 + 4 테스트 = **~8개**

---

## Phase 4: UseCase 인터페이스 추출 (High)

**목표**: 9개 서비스에 Inbound Port(UseCase) 인터페이스 추가, 컨트롤러가 구체 클래스 대신 인터페이스 의존

**이유**: 헥사고날 아키텍처의 Dependency Inversion 원칙 일관 적용

### 4a. 단순 조회 서비스 (5개) — 낮은 난이도

| 서비스 | 신규 인터페이스 | 메서드 |
|--------|----------------|--------|
| `SeasonService` | `SeasonQueryUseCase` | `getAllSeasons()` |
| `VersionService` | `VersionQueryUseCase` | `getLatestVersion()`, `getAllVersions()` |
| `PatchNoteService` | `PatchNoteQueryUseCase` | `getAllPatchNotes()`, `getPatchNoteByVersionId()` |
| `TierCutoffService` | `TierCutoffQueryUseCase` | `getTierCutoffsByRegion()` 등 |
| `SpectatorService` | `SpectatorQueryUseCase` | `getCurrentGameInfo()` |

**패턴** (각 서비스 동일):
1. `port/in/{Name}QueryUseCase.java` 인터페이스 생성
2. 서비스 클래스에 `implements {Name}QueryUseCase` 추가
3. 컨트롤러에서 `private final {Name}QueryUseCase` 주입으로 변경
4. 컨트롤러 테스트에서 mock 대상 변경

**파일 수**: 5 신규 인터페이스 + 5 서비스 수정 + 5 컨트롤러 수정 + 5 테스트 = **~20개**

### 4b. 복합 서비스 (4개) — 중간 난이도

| 서비스 | 신규 인터페이스 | 비고 |
|--------|----------------|------|
| `MatchService` | `MatchQueryUseCase` | 전체 조회 전용 |
| `LeagueService` | `LeagueQueryUseCase` | 전체 조회 전용 |
| `ChampionStatsService` | `ChampionStatsQueryUseCase` | 전체 조회 전용 |
| `SummonerService` | `SummonerUseCase` + `SummonerQueryUseCase` | 커맨드/쿼리 분리 필요 |

**파일 수**: ~6 인터페이스 + 4 서비스 + 6 컨트롤러 + 6 테스트 = **~22개**

**검증**: 컨트롤러에서 구체 `*Service` import가 0건인지 확인

---

## Phase 5: `@Configuration` 클래스 Core에서 이동 (Medium)

**목표**: 인프라 관심사인 Spring 설정 클래스를 application 모듈(composition root)로 이동

### 변경 내용

**이동 대상 (3개)**:
- `config/ExecutorConfig.java` → `module/app/application/.../config/`
- `config/CacheConfig.java` → 동일
- `config/CacheScheduler.java` → 동일

**테스트 이동 (3개)**:
- 대응하는 테스트 파일도 application 테스트 모듈로 이동

**검증**: `ls module/core/lol-server-domain/.../config/` → 빈 디렉토리 또는 삭제

**파일 수**: 3 이동 + 3 테스트 이동 = **~6개**

---

## 리팩토링하지 않는 항목 (의도적 제외)

| 항목 | 제외 이유 |
|------|-----------|
| **Match 도메인 `@Setter`** (ParticipantData, GameInfoData 등) | Riot API 응답 매핑용 데이터 컨테이너. 도메인 불변식 없음. 제거 시 매퍼 파이프라인 대규모 재작성 필요. **ROI 매우 낮음** |
| **Command DTO `@Setter`** (MatchCommand 등) | Spring MVC `@ModelAttribute` 바인딩에 setter 필요. CLAUDE.md에서 커맨드에는 허용 |
| **Community 트랜잭션 경계** (Vote→Post count 갱신) | 정석 해결은 도메인 이벤트 + eventual consistency + CQRS. 현재 단일 BC 내 실용적 접근. **이 규모에서 과도한 추상화** |
| **Cross-context 읽기 의존** (PostService→MemberPersistencePort) | 읽기 전용 포트 의존. Anti-corruption Layer 추가 시 복잡도만 증가. **모놀리스에서 수용 가능** |
| **도메인 이벤트 도입** | 현재 비동기 필요 케이스는 SummonerMessagePort 1건뿐. 도메인 이벤트 인프라 구축은 **시기상조** |
| **jackson-annotations 제거** | 사용 범위 조사 후 별도 진행 가능. 현재 우선순위 낮음 |

---

## 실행 전략: 병렬 처리

### Step 1 (순차 — 선행 작업)
- **Phase 0**: `Page` → `SliceResult` 리네이밍 (15개 파일)
  - 모든 후속 Phase가 이 리네이밍에 의존하므로 먼저 완료

### Step 2 (병렬 — 4개 동시 실행)

Phase 0 완료 후, 다음 4개를 **병렬 Agent로 동시 실행**:

| Agent | Phase | 작업 | 파일 수 | 겹치는 파일 |
|-------|-------|------|---------|-------------|
| Agent A | Phase 1+2 | Spring Data 제거 (Rank + Match) + build.gradle 정리 | ~14 | 없음 (rank, match context만) |
| Agent B | Phase 3 | Summoner `@Setter` 제거 | ~8 | 없음 (summoner context만) |
| Agent C | Phase 4a | 단순 조회 서비스 UseCase 추출 (5개) | ~20 | 없음 (season, version, patchnote, tiercutoff, spectator) |
| Agent D | Phase 5 | `@Configuration` 이동 | ~6 | 없음 (config 패키지만) |

**Phase 4b** (SummonerService, MatchService, LeagueService, ChampionStatsService UseCase 추출)는 Phase 1+2, Phase 3과 파일이 겹칠 수 있으므로 **Step 2 완료 후 순차 실행**.

### Step 3 (순차 — 마무리)
- **Phase 4b**: 복합 서비스 UseCase 추출 (4개, ~22개 파일)
- 전체 빌드 검증: `./gradlew test`

### 병렬 가능 근거

각 Agent가 수정하는 파일이 **완전히 독립된 Context/패키지**에 속함:
- Agent A: `domain/rank/`, `domain/match/`, `support/PageResult.java`, `support/PaginationRequest.java`
- Agent B: `domain/summoner/`, `repository/summoner/`, `client/lol-repository/.../summoner/`
- Agent C: `domain/season/`, `domain/version/`, `domain/patchnote/`, `domain/tiercutoff/`, `domain/spectator/` + 대응 컨트롤러
- Agent D: `config/` 패키지

**파일 충돌 0건** → 안전하게 병렬 실행 가능

---

## 총 영향 범위

| Phase | 우선순위 | 파일 수 | 핵심 성과 |
|-------|---------|---------|-----------|
| 0 | Critical | ~15 | `Page` → `SliceResult` 리네이밍, 이름 충돌 해소 |
| 1 | Critical | ~8 | Rank 포트에서 Spring Data 제거 |
| 2 | Critical | ~6 | **`spring-data-commons` 의존성 완전 삭제** |
| 3 | High | ~8 | Summoner Aggregate 불변식 보호 |
| 4 | High | ~42 | 모든 컨트롤러가 UseCase 인터페이스 의존 |
| 5 | Medium | ~6 | Core 모듈에서 @Configuration 제거 |
| **합계** | | **~85** | **Core 모듈 인프라 독립성 달성** |

## 검증 방법

각 Phase 완료 후:
1. `./gradlew :module:core:lol-server-domain:compileJava` — 컴파일 성공
2. `./gradlew test` — 전체 테스트 통과
3. Phase 1+2 완료 시: `grep -r "org.springframework.data" module/core/lol-server-domain/src/main/` → **0건**
4. Phase 3 완료 시: `grep -r "@Setter" module/core/lol-server-domain/src/main/java/.../domain/summoner/` → **0건**
