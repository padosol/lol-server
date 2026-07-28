# ADR 0001: 익명 기반 듀오 매칭 — 상호 동의 전까지 신원 비공개

## 상태
Accepted (2026-06-04)

## 컨텍스트

이 서비스는 리그 오브 레전드 전적 검색에서 출발했지만, 전적 검색 자체는 이미
시장에 다수 존재하는 기능이다. **이 프로젝트가 풀어야 할 고유한 문제**가 무엇인지를
정의하지 않으면 기존 서비스의 클론에 머문다.

기존 듀오 찾기 서비스들의 공통된 문제는 **신원이 공개된 채로 모집된다**는 점이다.

- 게시글에 라이엇 ID(게임이름·태그라인)가 그대로 노출된다.
- 글을 올리는 순간 **누구나** 인게임에서 친구추가를 걸 수 있다.
- 실제로 한 게시글에 원치 않는 친구추가·쪽지가 몰려 "무서울 정도"라는 사용자 피드백이 있었다.
  모집자가 통제할 수 없는 일방적 접근(친구추가/스토킹/하렘)이 구조적으로 발생한다.

즉 문제의 본질은 "매칭이 성사되기도 전에 신원이 공개된다"는 데 있다. 매칭은
양쪽의 합의여야 하는데, 기존 모델은 한쪽(모집자)의 신원을 선공개한 뒤 불특정 다수의
일방적 접근을 허용한다.

## 결정

**상호 동의(게시글 소유자의 수락 → 요청자의 확정) 이전에는 사용자 신원(라이엇 ID)을
어떤 화면·API에도 노출하지 않는다. 매칭은 라이엇이 검증한 실력 정보만으로 이루어지고,
신원은 양쪽이 합의한 순간에만 교환된다.**

이것이 이 프로젝트의 존재 이유다. 전적 검색은 이 매칭을 신뢰 가능하게 만드는 토대이고,
차별점은 "익명 기반 듀오 매칭"이다.

## 결과

### 익명성 계약 (단계별 공개 범위)

| 단계 | 상태 | 공개되는 것 | 비공개 |
|---|---|---|---|
| 게시글 작성 / 목록 / 상세 | ACTIVE | 티어·랭크·LP·라인·마이크·메모·모스트 챔피언·최근 전적 요약 | **라이엇 ID, 식별 정보 전부** |
| 요청 생성 | PENDING | 요청자의 실력 정보(위와 동일) | 라이엇 ID |
| 소유자 수락 | ACCEPTED | — (양쪽 ID 여전히 비공개) | 라이엇 ID |
| 요청자 확정 | CONFIRMED | **이때 처음 양쪽 라이엇 ID(게임이름·태그라인) 교환** | — |

핵심 불변식: **ID는 공개 게시글에 절대 존재하지 않으며, CONFIRMED 이전 어떤 응답에도
포함되지 않는다.** 실력 정보(티어/모스트 등)는 라이엇 API로 서버가 자동 조회한 검증값만
사용하고, 사용자가 자기 신원을 적을 수 있는 자유 입력(메모)에는 정책적으로 ID를 쓰지
않도록 가이드/필터링한다.

### 코드 / 테스트 경로

- 수락 단계 — 파트너 정보 미포함:
  `DuoRequestService.acceptDuoRequest()` →
  `DuoMatchResultModel.of(duoPost, duoRequest)` (partnerSummoner 없음)
  (`module/domain/duo/src/main/java/com/example/lolserver/duo/application/DuoRequestService.java:76`)
- 확정 단계 — 이때만 파트너 신원 조회·공개:
  `DuoRequestService.confirmDuoRequest()` →
  `summonerQueryUseCase` 로 파트너 조회 후
  `DuoMatchResultModel.of(duoPost, duoRequest, partnerSummoner)`
  (`module/domain/duo/src/main/java/com/example/lolserver/duo/application/DuoRequestService.java:93`)
- 실력 정보(신뢰 앵커) 자동 조회:
  `RiotAccountResolver.lookupAllStats()`
  (`module/domain/duo/src/main/java/com/example/lolserver/duo/application/RiotAccountResolver.java`)
- 공개 응답 DTO(ID 미포함 확인 지점):
  `DuoPostResponse` / `DuoPostListResponse` / `DuoPostDetailResponse`
  (`module/domain/duo/src/main/java/com/example/lolserver/duo/adapter/in/web/response/`)
- 파트너 신원 공개 응답: `DuoMatchResultResponse`
  (`module/domain/duo/src/main/java/com/example/lolserver/duo/adapter/in/web/response/DuoMatchResultResponse.java`)
- API 스펙 전체: `docs/duo-api-spec.md`
- 테스트: 현재 메인 모듈에 듀오 테스트가 없다. 익명성 계약을 회귀로 고정하는 테스트
  (공개 응답에 ID 미포함 / accept 시 파트너 정보 null / confirm 시에만 공개)를 추가해야 한다.

### 감수하는 비용 — 남은 작업

이 결정을 "진짜 익명"으로 내세우려면 아래 누수/공백을 닫아야 한다. (별도 ADR/작업으로 진행)

1. **통계 기반 역추적(de-anonymization).** 가장 큰 누수. 상위 티어에서
   `티어 + LP + 모스트 챔피언`은 사실상 지문이라 ID 없이도 op.gg 역검색으로 특정 가능.
   대응 후보: 상위 티어 LP 구간화, 모스트 챔피언 노출 범위 제한, 정확 LP는 매칭 후 공개.
2. **확정 이후의 괴롭힘.** CONFIRMED 후 ID를 넘기므로 그 뒤 친구추가/스토킹에 대한
   차단·신고·재매칭 거부 장치가 필요. (현재 API에 없음)
3. **익명 ↔ 신뢰 균형.** 익명일수록 티어 사칭·트롤·부계정을 거르기 어렵다.
   "라이엇 검증 스탯"을 신뢰 앵커로 유지하고, 동일인 반복 등록 시 평판을 쌓을 최소한의
   가명 식별자를 고민한다.

## 대안

- **모델 B — 라이엇 ID 영구 비공개 + 인앱 채널** (매칭돼도 ID를 주지 않고 인앱 채팅/
  일회성 파티 코드로만 연결) → **보류.** 익명성은 가장 강하지만 실시간 채팅/메시징
  인프라가 필요해 비용이 크다. 현 단계에서는 과투자.
- **모델 C — 하이브리드(기본 인앱 채널, 양측 동의 시 ID 교환)** → **보류.**
  B의 인프라를 전제로 하므로 같은 이유로 보류. 추후 B가 도입되면 자연스러운 확장 경로.
- **모델 A 채택 사유:** 신원의 *공개 노출*(문제의 핵심)은 이미 구조적으로 차단되며,
  추가 인프라 없이 누수 보완(위 1~3)만으로 "익명 기반"을 충분히 충족한다. 가장 적은
  비용으로 차별점을 완성한다.
