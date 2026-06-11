# ADR 0002: 듀오 매칭 플로우 — 3-way handshake · 선착순 확정 · 양방향 공개

## 상태
Accepted (2026-06-12)

## 컨텍스트

ADR 0001에서 "상호 동의 전까지 신원 비공개"라는 원칙을 정했다. 이를 실제 동작하는
매칭 흐름으로 구체화하려면 게시글·요청의 상태 전이, 동시 승락 처리, 정보 공개 시점,
만료/정리 규칙을 확정해야 한다.

요구사항 8개를 현재 코드와 대조한 결과: 일부는 이미 구현(3-way handshake, 다수 요청/승인,
RSO 연동 게이트)되어 있고, 일부는 누락(활성글 1개 제한, 양방향 공개, 선착순 동시성 제어,
만료 스케줄러)되거나 의미가 어긋나(탈락 요청 상태) 있었다. 본 ADR은 빠진 결정을 메우고
흐름 전체를 확정한다.

## 결정

듀오 매칭을 아래 흐름으로 한다. (R1~R8 = 요구사항 번호)

### 전제
- **R1** 회원 + Riot RSO 연동이 있어야 한다. **게시글 작성과 요청 보내기 둘 다** RSO 필수.

### 게시글
- **R2** 활성 게시글은 회원당 **최대 1개**. 이미 있으면 새 글 생성은 `409`로 거부한다.
  - `활성 = status==ACTIVE AND now < expiresAt`.
- **만료** `@Scheduled` 잡이 만료된 `ACTIVE → EXPIRED`로 전환한다. 활성글 카운트·목록 노출은
  EXPIRED를 제외한다.

### 요청 — 3-way handshake (TCP 비유)
- **R3** 한 게시글에 여러 명이 요청할 수 있다. 동일인 중복(PENDING/ACCEPTED)만 차단.
- **R4** `SYN` 요청 생성 → `PENDING`  ·  `SYN-ACK` 작성자 승인 → `ACCEPTED`  ·
  `ACK` 승인받은 요청자 승락 → `CONFIRMED`(매칭 성사).
- **R5** 작성자는 여러 요청을 승인(ACCEPTED)할 수 있다.
- **R7** **선착순**: 동시 승락 시 **Redisson 분산 락**(key=`duoPost:{postId}`)으로 단 1명만
  성사. `markMatched`는 게시글이 ACTIVE일 때만 허용(이미 MATCHED면 "이미 매칭됨" 에러).
- **R6** **양방향 공개**: 성사 시 요청자↔작성자가 **서로** 라이엇 ID(게임이름/태그라인)를
  공개받는다. 작성자가 매칭된 상대의 ID를 조회하는 경로를 신설한다. (CONFIRMED 이전엔
  여전히 양쪽 모두 비공개 — ADR 0001 익명성 계약 유지.)
- **R8** 성사되면 그 게시글의 **나머지 열린 요청(PENDING/ACCEPTED)을 전용 종료 상태로 닫는다.**

### 파생 규칙 (본 ADR에서 함께 확정)
- 게시글이 ACTIVE에서 벗어나면(EXPIRED/DELETED/MATCHED) 그 글의 **열린 요청도 함께 닫는다.**
  안 그러면 요청자가 무한 대기 상태로 오인한다.
- "1개 제한"은 **게시글**에만 적용된다. 한 유저가 자기 글을 가진 채 **남의 글에 요청**하는 것은 허용.

### 알림
- 승인/매칭/탈락을 **실시간 푸시**로 통지한다. 전송은 **SSE**(서버→클라이언트 단방향).
- 다중 인스턴스(k8s)에서 승락을 처리한 인스턴스와 요청자의 SSE 연결 인스턴스가 다를 수 있으므로
  **Redis pub/sub fanout**으로 이벤트를 브로드캐스트한다.

### 상태 enum 변경
- `DuoRequestStatus`에 **전용 종료 상태**를 추가한다(매칭으로 인한 자동 탈락 ≠ 작성자 거절
  REJECTED ≠ 본인 취소 CANCELLED). 명칭은 구현 시 확정(후보: `CLOSED`).
- `DuoPostStatus.EXPIRED`를 실제로 사용한다(만료 스케줄러).

## 결과

### 변경/추가 지점 (코드 경로)
- 활성글 1개 검증: `DuoService.createDuoPost()` + 신규 포트 `DuoPostPersistencePort.existsActiveByMemberId()`
  (`module/domain/duo/.../application/DuoService.java:40`)
- 선착순 분산 락 + markMatched 가드: `DuoRequestService.confirmDuoRequest()`
  (`module/domain/duo/.../application/DuoRequestService.java:93`), `DuoPost.markMatched()`
  (`module/domain/duo/.../domain/DuoPost.java:92`)
- 양방향 공개: 작성자용 매칭 결과 조회 경로 신설(컨트롤러/유스케이스/응답 DTO)
- 탈락 종료 상태: `DuoRequestStatus` enum + `DuoRequestPersistenceAdapter.rejectAllPendingAndAccepted()`
  를 신규 상태로 전환하도록 변경
- 만료 스케줄러: `@Scheduled` 잡 신설(기존 `config/CacheScheduler.java` 패턴 참고)
- SSE: 신규 어댑터(`adapter/in/web`의 SSE 엔드포인트) + Redis pub/sub fanout
- 테스트: 상태 전이/선착순 동시성/양방향 공개/활성글 1개 회귀 테스트 추가(현재 메인 모듈에 듀오 테스트 없음)

### 감수하는 비용
- 실시간 푸시(SSE + Redis pub/sub)는 매칭 로직보다 인프라·복잡도가 크다 — 본 기능의 최대 작업 비중.
- 분산 락은 Redis 가용성에 의존한다(이미 Redisson 사용 중이므로 신규 의존은 아님).

## 대안
- **선착순 제어:** 게시글 행 비관적 락 / 낙관적 락(@Version) 검토 → **Redisson 분산 락 채택.**
  (락 범위를 postId로 명시 제어, 기존 Redisson 인프라 재사용)
- **탈락 요청 상태:** REJECTED 재사용 / CANCELLED 통일 검토 → **전용 종료 상태 신설 채택.**
  (거절·본인취소·자동탈락을 구분해 UX/분석 정확도 확보)
- **알림:** 클라이언트 폴링 / 경량 상태 조회 API 검토 → **SSE 실시간 푸시 채택.** 선착순 공정성↑.
- **푸시 전송:** WebSocket → **보류.** 현재는 단방향이라 SSE로 충분. 향후 인앱 채팅(ADR 0001 모델 B)
  도입 시 양방향 전송으로 재검토.
