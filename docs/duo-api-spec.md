# 듀오 찾기 API 스펙 (프론트엔드용)

## 기본 정보
- Base URL: `/api/duo`
- 인증: `Authorization: Bearer <token>` (일부 조회 API는 비인증 허용)
- 응답 형식: `{ "result": "SUCCESS", "errorMessage": null, "data": {...} }`

---

## 1. 게시글 API

### POST /api/duo/posts — 게시글 생성 (인증 필수)
```json
// Request
{ "primaryLane": "MID", "secondaryLane": "JUNGLE", "hasMicrophone": true, "memo": "듀오 구합니다" }

// Response data
{ "id": 1, "primaryLane": "MID", "secondaryLane": "JUNGLE", "hasMicrophone": true,
  "tier": "GOLD", "rank": "I", "leaguePoints": 50, "memo": "듀오 구합니다",
  "status": "ACTIVE", "tierAvailable": true, "expiresAt": "2026-04-15T10:00:00", "createdAt": "2026-04-14T10:00:00" }
```
- tier/rank/leaguePoints는 서버가 Riot API에서 자동 조회
- 게시글 유효시간: 24시간 (expiresAt)
- Riot 계정 연동 필수 (미연동 시 400 에러)

### GET /api/duo/posts — 게시글 목록 조회 (인증 불필요)
```
Query: ?lane=MID&tier=GOLD&page=0 (모두 선택사항)
```
```json
// Response data
{ "content": [
    { "id": 1, "primaryLane": "MID", "secondaryLane": "JUNGLE", "hasMicrophone": true,
      "tier": "GOLD", "rank": "I", "leaguePoints": 50, "memo": "듀오 구합니다",
      "status": "ACTIVE", "requestCount": 3, "expiresAt": "...", "createdAt": "..." }
  ], "hasNext": true }
```
- lane 값: TOP, JUNGLE, MID, ADC, SUPPORT
- tier 값: IRON, BRONZE, SILVER, GOLD, PLATINUM, EMERALD, DIAMOND, MASTER, GRANDMASTER, CHALLENGER

### GET /api/duo/posts/{postId} — 게시글 상세 조회
```json
// Response data
{ "id": 1, "primaryLane": "MID", "secondaryLane": "JUNGLE", "hasMicrophone": true,
  "tier": "GOLD", "rank": "I", "leaguePoints": 50, "memo": "듀오 구합니다",
  "status": "ACTIVE", "isOwner": true, "expiresAt": "...", "createdAt": "...",
  "requests": [
    { "id": 10, "duoPostId": 1, "primaryLane": "ADC", "secondaryLane": "SUPPORT",
      "hasMicrophone": false, "tier": "SILVER", "rank": "II", "leaguePoints": 30,
      "memo": "같이 하실 분", "status": "PENDING", "createdAt": "..." }
  ] }
```
- `isOwner=true`일 때만 `requests` 배열에 데이터 포함
- 비소유자/비로그인 시 `requests`는 빈 배열

### PUT /api/duo/posts/{postId} — 게시글 수정 (인증 필수, 소유자만)
```json
// Request (생성과 동일)
{ "primaryLane": "TOP", "secondaryLane": "SUPPORT", "hasMicrophone": false, "memo": "수정된 메모" }
// Response: 생성 응답과 동일 구조
```
- ACTIVE 상태일 때만 수정 가능

### DELETE /api/duo/posts/{postId} — 게시글 삭제 (인증 필수, 소유자만)
```json
// Response
{ "result": "SUCCESS", "errorMessage": null, "data": null }
```

### GET /api/duo/me/posts — 내 게시글 목록 (인증 필수)
```
Query: ?page=0
```
- 응답: 목록 조회와 동일 구조

---

## 2. 매칭 요청 API

### POST /api/duo/posts/{postId}/requests — 요청 생성 (인증 필수)
```json
// Request
{ "primaryLane": "ADC", "secondaryLane": "SUPPORT", "hasMicrophone": false, "memo": "같이 하실 분" }

// Response data
{ "id": 10, "duoPostId": 1, "primaryLane": "ADC", "secondaryLane": "SUPPORT",
  "hasMicrophone": false, "tier": "SILVER", "rank": "II", "leaguePoints": 30,
  "memo": "같이 하실 분", "status": "PENDING", "createdAt": "..." }
```
- 자기 게시글에 요청 불가 (400)
- 이미 PENDING/ACCEPTED 상태 요청이 있으면 중복 불가 (409)
- 비활성 게시글에 요청 불가 (400)

### GET /api/duo/posts/{postId}/requests — 게시글 요청 목록 (인증 필수, 소유자만)
```json
// Response data (배열)
[ { "id": 10, "duoPostId": 1, ... , "status": "PENDING", "createdAt": "..." } ]
```

### PUT /api/duo/requests/{requestId}/accept — 요청 수락 (게시글 소유자)
```json
// Response data
{ "duoPostId": 1, "requestId": 10, "partnerGameName": null, "partnerTagLine": null, "status": "ACCEPTED" }
```
- 수락 단계에서는 파트너 정보 미공개

### PUT /api/duo/requests/{requestId}/confirm — 요청 확정 (요청자)
```json
// Response data
{ "duoPostId": 1, "requestId": 10, "partnerGameName": "Hide on bush", "partnerTagLine": "KR1", "status": "CONFIRMED" }
```
- 확정 시 파트너 게임이름/태그라인 공개
- 게시글 상태 → MATCHED, 다른 요청 자동 거절

### PUT /api/duo/requests/{requestId}/reject — 요청 거절 (게시글 소유자)
```json
// Response: { "result": "SUCCESS", "errorMessage": null, "data": null }
```

### PUT /api/duo/requests/{requestId}/cancel — 요청 취소 (요청자)
```json
// Response: { "result": "SUCCESS", "errorMessage": null, "data": null }
```

### GET /api/duo/me/requests — 내 요청 목록 (인증 필수)
```
Query: ?page=0
```
```json
// Response data
{ "content": [ { "id": 10, "duoPostId": 1, ... } ], "hasNext": false }
```

---

## 3. 매칭 플로우

```
[게시글 작성] → ACTIVE
         ↓
[다른 사용자가 요청] → PENDING
         ↓
[게시글 소유자가 수락] → ACCEPTED
         ↓
[요청자가 확정] → CONFIRMED (파트너 정보 공개)
  ├── 게시글 → MATCHED
  └── 다른 요청 → 자동 REJECTED
```

### 게시글 상태: ACTIVE → MATCHED / DELETED / EXPIRED(24시간)
### 요청 상태: PENDING → ACCEPTED → CONFIRMED / REJECTED / CANCELLED

---

## 4. 에러 코드

| 에러 | HTTP | 설명 |
|------|------|------|
| RIOT_ACCOUNT_NOT_LINKED | 400 | Riot 계정 미연동 |
| DUO_POST_NOT_FOUND | 404 | 게시글 없음 |
| DUO_POST_NOT_ACTIVE | 400 | 비활성 게시글 |
| DUO_POST_SELF_REQUEST | 400 | 자기 게시글에 요청 |
| DUO_REQUEST_NOT_FOUND | 404 | 요청 없음 |
| DUO_REQUEST_ALREADY_EXISTS | 409 | 중복 요청 |
| FORBIDDEN | 403 | 권한 없음 |
| INVALID_LANE | 400 | 잘못된 라인 값 |
