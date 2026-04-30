# 듀오 찾기(DuoFinder) 기능 구현 계획

## Context

리그 오브 레전드 전적 검색 서비스에 **듀오 찾기** 기능을 추가한다. Riot 연동된 유저끼리 익명으로 듀오 파트너를 찾고, 3-handshake 방식으로 매칭을 확정한 뒤 서로의 gameName을 공개하는 기능이다.

### 비즈니스 플로우
```
A(등록) → B(요청) → A(수락) → B(확인) → 매칭 완료 (gameName 공개)
```
- 1:N: 한 게시글에 여러 요청 가능
- A가 하나를 수락해도 나머지 요청은 대기 유지 (B가 확인 전까지 A가 다른 요청도 수락 가능)
- B가 확인하면 매칭 확정 → 게시글 MATCHED + 나머지 요청 자동 REJECTED
- 게시글에 만료 시간 (24시간), 조회 시 lazy expiration 전략

---

## Phase 1: Domain Layer

### 1.1 Value Objects — `domain/duo/domain/vo/`

**Lane.java** (enum)
```
TOP, JUNGLE, MID, ADC, SUPPORT, FILL
```

**DuoPostStatus.java** (enum)
```
ACTIVE → MATCHED (매칭 확정)
ACTIVE → EXPIRED (만료)
ACTIVE → DELETED (소프트 딜리트)
```

**DuoRequestStatus.java** (enum)
```
PENDING → ACCEPTED (A 수락)
PENDING → REJECTED (A 거절 / 자동 거절)
PENDING → CANCELLED (B 취소)
ACCEPTED → CONFIRMED (B 확인 → 매칭 완료)
ACCEPTED → REJECTED (자동 거절)
ACCEPTED → CANCELLED (B 취소)
```

**TierInfo.java** (record)
- `tier`, `rank`, `leaguePoints` — 티어 조회 결과를 담는 VO

### 1.2 Domain Objects — `domain/duo/domain/`

**DuoPost.java** (Aggregate Root)
- 필드: `id`, `memberId`, `puuid`, `primaryLane(Lane)`, `secondaryLane(Lane)`, `hasMicrophone`, `tier`, `rank`, `leaguePoints`, `memo`, `status(DuoPostStatus)`, `expiresAt`, `createdAt`, `updatedAt`
- 팩토리: `DuoPost.create(memberId, puuid, primaryLane, secondaryLane, hasMicrophone, tier, rank, leaguePoints, memo)` — status=ACTIVE, expiresAt=now+24h
- 메서드: `isOwner(memberId)`, `isActive()`, `markMatched()`, `markDeleted()`, `markExpired()`

**DuoRequest.java** (별도 Aggregate)
- 필드: `id`, `duoPostId`, `requesterId`, `requesterPuuid`, `primaryLane(Lane)`, `secondaryLane(Lane)`, `hasMicrophone`, `tier`, `rank`, `leaguePoints`, `memo`, `status(DuoRequestStatus)`, `createdAt`, `updatedAt`
- 팩토리: `DuoRequest.create(...)` — status=PENDING
- 메서드: `isRequester(memberId)`, `accept()`, `confirm()`, `reject()`, `cancel()` — 각 메서드에 상태 전이 검증 포함

> Aggregate 분리 이유: 여러 B가 동시 요청 시 DuoPost 내부에 Request를 두면 낙관적 락 충돌 빈번. 별도 Aggregate로 독립 저장.

### 1.3 Commands — `domain/duo/application/command/`

- **CreateDuoPostCommand**: `primaryLane`, `secondaryLane`, `hasMicrophone`, `memo`
- **CreateDuoRequestCommand**: `primaryLane`, `secondaryLane`, `hasMicrophone`, `memo`
- **DuoPostSearchCommand**: `lane`(필터), `tier`(필터), `page`

### 1.4 ReadModels — `domain/duo/application/model/`

- **DuoPostReadModel**: 등록 결과 (tierAvailable 포함 — false이면 "갱신 필요" 안내)
- **DuoPostListReadModel**: 목록 조회 (gameName 비공개, requestCount 포함)
- **DuoPostDetailReadModel**: 상세 조회 (isOwner이면 requests 목록 포함)
- **DuoRequestReadModel**: 요청 정보
- **DuoMatchResultReadModel**: 매칭 결과 (`partnerGameName`, `partnerTagLine`, `status`)

### 1.5 Ports — `domain/duo/application/port/`

**In Ports:**
- `DuoPostUseCase`: `createDuoPost`, `deleteDuoPost`
- `DuoPostQueryUseCase`: `getDuoPosts`, `getDuoPost`, `getMyDuoPosts`
- `DuoRequestUseCase`: `createDuoRequest`, `acceptDuoRequest`, `confirmDuoRequest`, `rejectDuoRequest`, `cancelDuoRequest`
- `DuoRequestQueryUseCase`: `getDuoRequestsForPost`, `getMyDuoRequests`

**Out Ports:**
- `DuoPostPersistencePort`: `save`, `findById`, `findActivePosts`, `findByMemberId`
- `DuoRequestPersistencePort`: `save`, `findById`, `findByDuoPostId`, `existsByDuoPostIdAndRequesterIdAndStatusIn`, `findByDuoPostIdAndStatusIn`, `rejectAllPendingAndAccepted`, `findByRequesterId`

**기존 포트 재사용 (수정 없음):**
- `MemberPersistencePort.findByIdWithSocialAccounts()` — Riot 연동 확인
- `LeaguePersistencePort.findAllLeaguesByPuuid()` — 티어 조회 (queue="RANKED_SOLO_5x5" 필터)
- `SummonerPersistencePort.findById()` — gameName/tagLine 조회

### 1.6 Service — `domain/duo/application/DuoService.java`

`@Service @Transactional(readOnly = true)`, UseCase 4개 모두 구현.

핵심 헬퍼:
- `extractRiotPuuid(memberId)`: Member → SocialAccount(RIOT) → puuid 추출, 없으면 RIOT_ACCOUNT_NOT_LINKED
- `lookupTierInfo(puuid)`: LeaguePersistencePort에서 RANKED_SOLO_5x5 티어 조회, 없으면 TierInfo.UNRANKED

`confirmDuoRequest` 핵심 로직:
1. request.confirm() → post.markMatched()
2. `rejectAllPendingAndAccepted(duoPostId, excludeRequestId)` bulk update
3. 양쪽 puuid로 SummonerPersistencePort에서 gameName/tagLine 조회 → DuoMatchResultReadModel 반환

---

## Phase 2: ErrorType 추가

파일: `module/core/lol-server-domain/.../support/error/ErrorType.java`

```java
RIOT_ACCOUNT_NOT_LINKED(400, ErrorCode.E400, "Riot 계정 연동이 필요합니다."),
DUO_POST_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 듀오 게시글입니다."),
DUO_POST_NOT_ACTIVE(400, ErrorCode.E400, "활성 상태의 듀오 게시글이 아닙니다."),
DUO_POST_SELF_REQUEST(400, ErrorCode.E400, "본인의 듀오 게시글에는 요청할 수 없습니다."),
DUO_REQUEST_NOT_FOUND(404, ErrorCode.E404, "존재하지 않는 듀오 요청입니다."),
DUO_REQUEST_ALREADY_EXISTS(409, ErrorCode.E409, "이미 해당 게시글에 요청을 보냈습니다."),
DUO_REQUEST_NOT_PENDING(400, ErrorCode.E400, "대기 상태의 요청만 수락할 수 있습니다."),
DUO_REQUEST_NOT_ACCEPTED(400, ErrorCode.E400, "수락된 요청만 확인할 수 있습니다."),
DUO_REQUEST_ALREADY_COMPLETED(400, ErrorCode.E400, "이미 처리 완료된 요청입니다."),
INVALID_LANE(400, ErrorCode.E400, "유효하지 않은 라인입니다."),
TIER_INFO_NOT_AVAILABLE(400, ErrorCode.E400, "티어 정보가 없습니다. 전적을 갱신해주세요."),
```

---

## Phase 3: Persistence Layer

### 3.1 Entities — `repository/duo/entity/`

**DuoPostEntity.java**
- `@Table(name = "duo_post")`, `@GeneratedValue(IDENTITY)`
- `rank` → 컬럼명 `tier_rank` (PostgreSQL 예약어)
- `primaryLane`, `secondaryLane`, `status`는 String으로 저장 (enum.name())

**DuoRequestEntity.java**
- `@Table(name = "duo_request")`
- 동일 패턴

### 3.2 Mappers — `repository/duo/mapper/`

**DuoPostMapper.java** (`componentModel = "spring"`)
- `toDomain(entity)`: String → enum 변환 (`Lane.valueOf`, `DuoPostStatus.valueOf`)
- `toEntity(domain)`: enum → String 변환 (`.name()`)

**DuoRequestMapper.java** — 동일 패턴

### 3.3 Repositories — `repository/duo/repository/`

**DuoPostJpaRepository.java**: 기본 CRUD + memberId 조회

**DuoRequestJpaRepository.java**:
- `findByDuoPostId`, `existsByDuoPostIdAndRequesterIdAndStatusIn`
- `@Modifying @Query rejectAllPendingAndAccepted(duoPostId, excludeRequestId)` — bulk update

**DuoPostRepositoryCustom.java** + **DuoPostRepositoryCustomImpl.java** (QueryDSL):
- `findActivePosts(lane, tier, pageable)` — status=ACTIVE & expires_at > now() 필터 + lane/tier 선택 필터

### 3.4 DTO — `repository/duo/dto/`

**DuoPostListDTO.java**: QueryDSL projection용 (requestCount 포함)

### 3.5 Adapters — `repository/duo/adapter/`

**DuoPostPersistenceAdapter.java**: `DuoPostPersistencePort` 구현
**DuoRequestPersistenceAdapter.java**: `DuoRequestPersistencePort` 구현

---

## Phase 4: API Layer

### 4.1 Controller — `controller/duo/`

**DuoPostController.java**

| Method | Endpoint | Auth | 설명 |
|--------|----------|------|------|
| POST | `/api/duo/posts` | Required | 게시글 등록 |
| GET | `/api/duo/posts` | Optional | 목록 조회 (lane, tier 필터) |
| GET | `/api/duo/posts/{postId}` | Optional | 상세 조회 |
| DELETE | `/api/duo/posts/{postId}` | Required | 삭제 (soft) |
| GET | `/api/duo/me/posts` | Required | 내 게시글 목록 |

**DuoRequestController.java**

| Method | Endpoint | Auth | 설명 |
|--------|----------|------|------|
| POST | `/api/duo/posts/{postId}/requests` | Required | 매칭 요청 |
| GET | `/api/duo/posts/{postId}/requests` | Required | 요청 목록 (A만) |
| PUT | `/api/duo/requests/{requestId}/accept` | Required | 수락 (A) |
| PUT | `/api/duo/requests/{requestId}/confirm` | Required | 확인 (B) |
| PUT | `/api/duo/requests/{requestId}/reject` | Required | 거절 (A) |
| PUT | `/api/duo/requests/{requestId}/cancel` | Required | 취소 (B) |
| GET | `/api/duo/me/requests` | Required | 내가 보낸 요청 목록 |

### 4.2 Request/Response DTOs — `controller/duo/request/`, `controller/duo/response/`

- Request: `CreateDuoPostRequest`, `CreateDuoRequestRequest` (record, `@NotBlank` 검증)
- Response: `DuoPostResponse`, `DuoPostListResponse`, `DuoPostDetailResponse`, `DuoRequestResponse`, `DuoMatchResultResponse` (record, `from()` 팩토리)

### 4.3 SecurityConfig 수정

```java
.requestMatchers(HttpMethod.GET, "/api/duo/posts/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/duo/posts").permitAll()
.requestMatchers("/api/duo/**").authenticated()
```

---

## Phase 5: DB Migration

파일: `lol-db-schema/db/migration/V__add_duo_tables.sql` (버전 번호는 기존 마이그레이션 확인 후 결정)

### duo_post 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (IDENTITY) | PK |
| member_id | BIGINT NOT NULL | FK → member |
| puuid | VARCHAR(50) NOT NULL | Riot puuid |
| primary_lane | VARCHAR(20) NOT NULL | 주 라인 |
| secondary_lane | VARCHAR(20) NOT NULL | 보조 라인 |
| has_microphone | BOOLEAN NOT NULL | 마이크 여부 |
| tier | VARCHAR(20) | 티어 (null=미등록) |
| tier_rank | VARCHAR(5) | 랭크 |
| league_points | INT DEFAULT 0 | LP |
| memo | VARCHAR(500) | 메모 |
| status | VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' | 상태 |
| expires_at | TIMESTAMP NOT NULL | 만료 시간 |
| created_at | TIMESTAMP NOT NULL | 생성일 |
| updated_at | TIMESTAMP NOT NULL | 수정일 |

인덱스: `(status, expires_at DESC) WHERE status='ACTIVE'`, `(member_id)`, `(primary_lane, status) WHERE status='ACTIVE'`

### duo_request 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (IDENTITY) | PK |
| duo_post_id | BIGINT NOT NULL | FK → duo_post |
| requester_id | BIGINT NOT NULL | FK → member |
| requester_puuid | VARCHAR(50) NOT NULL | 요청자 puuid |
| primary_lane | VARCHAR(20) NOT NULL | 주 라인 |
| secondary_lane | VARCHAR(20) NOT NULL | 보조 라인 |
| has_microphone | BOOLEAN NOT NULL | 마이크 여부 |
| tier | VARCHAR(20) | 티어 |
| tier_rank | VARCHAR(5) | 랭크 |
| league_points | INT DEFAULT 0 | LP |
| memo | VARCHAR(500) | 메모 |
| status | VARCHAR(20) NOT NULL DEFAULT 'PENDING' | 상태 |
| created_at | TIMESTAMP NOT NULL | 생성일 |
| updated_at | TIMESTAMP NOT NULL | 수정일 |

제약: `UNIQUE(duo_post_id, requester_id)` — 한 게시글당 한 유저 하나의 요청만
인덱스: `(duo_post_id, status)`, `(requester_id)`

---

## Phase 6: Tests

1. **DuoServiceTest.java** — Mockito 단위 테스트
   - 등록: Riot 미연동 시 에러, 정상 등록, 티어 없을 때 tierAvailable=false
   - 요청: 자기 게시글 요청 시 에러, 중복 요청 에러, 비활성 게시글 요청 에러
   - 수락/확인/거절/취소: 상태 전이 검증, 매칭 확정 시 나머지 자동 거절
   
2. **DuoPostMapperTest.java** — MapStruct 매퍼 테스트
3. **DuoRequestMapperTest.java** — MapStruct 매퍼 테스트

---

## 구현 순서

1. VO (Lane, DuoPostStatus, DuoRequestStatus, TierInfo)
2. Domain (DuoPost, DuoRequest)
3. Command, ReadModel
4. Port (In/Out)
5. ErrorType 추가
6. DuoService
7. Entity, Mapper, Repository, QueryDSL
8. Adapter
9. Controller Request/Response DTOs
10. Controller
11. SecurityConfig 수정
12. DB Migration
13. Tests

## 검증 방법

1. `./gradlew :module:core:lol-server-domain:test` — 도메인 단위 테스트
2. `./gradlew :module:infra:persistence:postgresql:test` — 매퍼 테스트
3. `./gradlew build` — 전체 빌드 성공 확인
