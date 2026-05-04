# Duo 도메인 헥사고날 아키텍처 분석 결과

## Context

duo 도메인이 프로젝트의 헥사고날 아키텍처 규칙(CLAUDE.md)을 얼마나 잘 준수하고 있는지 분석한 결과입니다.

---

## 전체 평가: 우수 (구조적 위반 없음, 크로스 도메인 결합 개선 필요)

duo 도메인은 헥사고날 아키텍처의 구조적 규칙(의존성 방향, 포트 분리, 도메인 순수성)을 정확히 따르고 있습니다.
다만 애플리케이션 서비스의 private 메서드에서 **타 도메인(Member, League) 내부 구조에 대한 지식 유출**이 발견되어 개선이 필요합니다.

---

## 1. 디렉토리 구조 - PASS

```
domain/duo/
├── application/
│   ├── DuoService.java                    # 애플리케이션 서비스
│   ├── command/                           # 입력 DTO
│   │   ├── CreateDuoPostCommand.java
│   │   ├── CreateDuoRequestCommand.java
│   │   ├── DuoPostSearchCommand.java
│   │   └── UpdateDuoPostCommand.java
│   ├── model/                             # ReadModel (불변)
│   │   ├── DuoMatchResultReadModel.java
│   │   ├── DuoPostDetailReadModel.java
│   │   ├── DuoPostListReadModel.java
│   │   ├── DuoPostReadModel.java
│   │   └── DuoRequestReadModel.java
│   └── port/
│       ├── in/                            # 입력 포트 (Use Case)
│       │   ├── DuoPostQueryUseCase.java
│       │   ├── DuoPostUseCase.java
│       │   ├── DuoRequestQueryUseCase.java
│       │   └── DuoRequestUseCase.java
│       └── out/                           # 출력 포트
│           ├── DuoPostPersistencePort.java
│           └── DuoRequestPersistencePort.java
└── domain/                                # 순수 도메인
    ├── DuoPost.java
    ├── DuoRequest.java
    └── vo/
        ├── DuoPostStatus.java
        ├── DuoRequestStatus.java
        ├── Lane.java
        └── TierInfo.java
```

`domain/`, `application/`, `port/in/`, `port/out/`, `model/`, `command/` 패키지가 프로젝트 규칙대로 정확히 분리되어 있음.

---

## 2. 도메인 객체 - PASS

### DuoPost.java
| 검증 항목 | 결과 | 비고 |
|-----------|------|------|
| 인프라 어노테이션 없음 | PASS | `@Entity`, `@Table` 등 없음 |
| `@Setter` 사용 안 함 | PASS | `@Getter` + `@Builder` + `@AllArgsConstructor(PRIVATE)` |
| `validate*` guard 메서드 | PASS | `validateOwner`, `validateNotOwner`, `validateActive` |
| 팩토리 메서드 | PASS | `DuoPost.create(...)` 정적 팩토리 |
| 상태 변경 메서드 | PASS | `markMatched()`, `markDeleted()`, `markExpired()`, `updateContent()` |

### DuoRequest.java
| 검증 항목 | 결과 | 비고 |
|-----------|------|------|
| 인프라 어노테이션 없음 | PASS | |
| `@Setter` 사용 안 함 | PASS | |
| `validate*` guard 메서드 | PASS | `validateRequester` |
| 팩토리 메서드 | PASS | `DuoRequest.create(...)` 정적 팩토리 |
| 상태 전이 guard | PASS | `accept()`, `confirm()`, `reject()`, `cancel()` 내부에서 상태 검증 |

### Value Objects
- `Lane` - enum + `Lane.from(String)` 팩토리 메서드 (유효하지 않은 값에 `CoreException` 발생)
- `TierInfo` - Java record (불변), `UNRANKED` 상수 제공
- `DuoPostStatus`, `DuoRequestStatus` - 순수 enum

---

## 3. 애플리케이션 서비스 (DuoService.java) - 부분 개선 필요

| 검증 항목 | 결과 | 비고 |
|-----------|------|------|
| 포트 인터페이스 구현 | PASS | 4개 Use Case 모두 구현 |
| 생성자 주입 | PASS | `@RequiredArgsConstructor` + `private final` |
| boolean 체크 + throw 금지 | PASS | 모든 도메인 검증은 `validate*` 메서드에 위임 |
| 트랜잭션 설정 | PASS | 클래스: `readOnly=true`, 변경 메서드: `@Transactional` |
| 크로스 도메인 접근 | **주의** | private 메서드에서 타 도메인 내부 구조에 직접 접근 |

**서비스의 도메인 검증 위임 패턴 (모두 올바름):**
- `duoPost.validateOwner(memberId)` - 라인 76, 89, 171, 214, 237
- `duoPost.validateActive()` - 라인 90, 138
- `duoPost.validateNotOwner(memberId)` - 라인 139
- `duoRequest.validateRequester(memberId)` - 라인 185, 226
- `duoRequest.accept()` / `confirm()` / `reject()` / `cancel()` - 내부 상태 검증 포함

**참고:** 라인 141-148의 중복 요청 체크(`alreadyRequested`)는 영속성 레벨 비즈니스 규칙으로, 아직 생성되지 않은 객체에 대한 검증이므로 서비스에서 처리하는 것이 적절함.

### 크로스 도메인 지식 유출 (private 메서드)

#### `extractRiotPuuid(Long memberId)` - 라인 249-258

```java
private String extractRiotPuuid(Long memberId) {
    Member member = memberPersistencePort.findByIdWithSocialAccounts(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));
    return member.getSocialAccounts().stream()
            .filter(sa -> OAuthProvider.RIOT.name().equals(sa.getProvider()) && sa.getPuuid() != null)
            .map(SocialAccount::getPuuid)
            .findFirst()
            .orElseThrow(() -> new CoreException(ErrorType.RIOT_ACCOUNT_NOT_LINKED));
}
```

**문제점:**
- DuoService가 Member 도메인의 내부 구조(`SocialAccount`, `OAuthProvider`)를 직접 탐색
- "RIOT 소셜 계정에서 puuid를 추출한다"는 로직은 **Member 도메인의 비즈니스 규칙**
- `Member` 클래스에 이미 소셜 계정 관련 메서드(`linkSocialAccount`, `unlinkSocialAccount`)가 있으므로 `getRiotPuuid()` 같은 도메인 메서드를 추가하는 것이 자연스러움

**개선 방안:**
- `Member` 도메인에 `getRiotPuuid()` 메서드 추가 → DuoService에서 `member.getRiotPuuid()` 호출
- 또는 `MemberPersistencePort`에 `findRiotPuuidByMemberId(Long memberId)` 전용 포트 메서드 추가

#### `lookupTierInfo(String puuid)` - 라인 260-267

```java
private TierInfo lookupTierInfo(String puuid) {
    return leaguePersistencePort.findAllLeaguesByPuuid(puuid).stream()
            .filter(league -> QueueType.RANKED_SOLO_5x5.name().equals(league.getQueue()))
            .findFirst()
            .map(league -> new TierInfo(league.getTier(), league.getRank(), league.getLeaguePoints()))
            .orElse(TierInfo.UNRANKED);
}
```

**문제점:**
- DuoService가 League 도메인의 내부 구조(`League.getQueue()`, `League.getTier()` 등)를 직접 탐색
- "솔로 랭크 리그를 찾아 티어 정보로 변환한다"는 로직은 **League 도메인의 비즈니스 규칙**
- 전체 리그 목록을 가져온 후 서비스에서 필터링하는 것은 비효율적이며 도메인 로직 유출

**개선 방안:**
- `LeaguePersistencePort`에 `findSoloRankByPuuid(String puuid)` 전용 메서드 추가
- 또는 League 도메인에 "솔로 랭크를 찾는" 비즈니스 로직을 캡슐화

#### 왜 문제인가?

| 관점 | 현재 | 개선 후 |
|------|------|---------|
| **캡슐화** | DuoService가 Member/League 내부 구조를 앎 | 각 도메인이 자신의 로직을 캡슐화 |
| **변경 영향** | Member/League 구조 변경 시 DuoService 수정 필요 | 도메인 메서드 시그니처만 유지하면 됨 |
| **중복 위험** | 동일 로직이 다른 서비스에서도 필요할 때 복사 | 도메인 메서드 재사용 가능 |
| **테스트** | DuoService 테스트에서 Member/League 내부 구조 mock 필요 | 단일 메서드 호출 mock으로 단순화 |

---

## 4. 포트 인터페이스 - PASS

| 검증 항목 | 결과 |
|-----------|------|
| 순수 인터페이스 (인프라 어노테이션 없음) | PASS |
| in/out 포트 분리 | PASS |
| Command/Query 분리 | PASS (UseCase vs QueryUseCase) |
| 도메인 객체로 입출력 | PASS |

---

## 5. ReadModel - PASS (개선 여지 있음)

| ReadModel | `final` 필드 | `of()` 팩토리 | Java Record |
|-----------|:---:|:---:|:---:|
| `DuoPostReadModel` | PASS | PASS | - Lombok |
| `DuoPostDetailReadModel` | PASS | PASS | - Lombok |
| `DuoRequestReadModel` | PASS | PASS | - Lombok |
| `DuoMatchResultReadModel` | PASS | PASS | - Lombok |
| `DuoPostListReadModel` | PASS | **없음** | - Lombok |

**관찰 사항:**
- `DuoPostListReadModel`에 `of()` 팩토리 메서드가 없음 - 이 ReadModel은 QueryDSL 프로젝션으로 직접 생성되어 out-port가 반환하기 때문
- 모든 ReadModel이 Lombok `@Getter` + `@Builder`이고 Java record가 아님 - `final` 필드로 사실상 불변이므로 기능적 문제는 없으나, CLAUDE.md의 "불변, Java Record" 선호 규칙과는 약간 다름

---

## 6. 의존성 방향 - PASS (위반 없음)

**core 모듈 → infra 모듈 방향의 의존성: 0건**

`DuoService.java` import 목록에서 인프라 관련 import:
- `org.springframework.stereotype.Service` - Spring 메타 어노테이션 (프로젝트 전체 일관된 선택)
- `org.springframework.transaction.annotation.Transactional` - 트랜잭션 관리 (프로젝트 전체 일관된 선택)

이 두 Spring 어노테이션은 기술적으로 인프라 결합이지만, 프로젝트 전체에서 일관되게 사용하는 아키텍처 결정이며 비즈니스 로직 자체는 인프라에 독립적임.

**infra 어댑터 → core 포트 방향: 올바름**
- `DuoPostPersistenceAdapter implements DuoPostPersistencePort`
- `DuoRequestPersistenceAdapter implements DuoRequestPersistencePort`
- 컨트롤러는 in-port(Use Case) 인터페이스에 의존

---

## 7. 인프라 어댑터 - PASS

| 어댑터 | 포트 구현 | 상태코드 | 응답 래핑 |
|--------|:---------:|:--------:|:---------:|
| API 컨트롤러 | Use Case 의존 | POST→201, GET→200, DELETE→204 | `ResponseEntity<ApiResponse<T>>` |
| PostgreSQL 어댑터 | PersistencePort 구현 | - | - |
| Redis | 미구현 | - | - |
| Message | 미구현 | - | - |

---

## 종합 결론

### 준수 항목
- 도메인 객체에 인프라 어노테이션 없음
- `@Setter` 사용 없음
- `validate*` guard 메서드로 도메인 검증 위임
- 팩토리 메서드/Builder 패턴 사용
- 포트 인터페이스 순수성 유지
- 의존성 방향 올바름 (infra → core)
- Command/Query 분리
- `@Transactional` 적절한 사용
- 매직 스트링 없음 (`QueueType.RANKED_SOLO_5x5.name()` 등 사용)

### 개선 필요 사항
1. **크로스 도메인 지식 유출 해소** (3장 참조)
   - `extractRiotPuuid`: Member 도메인에 `getRiotPuuid()` 도메인 메서드 추가
   - `lookupTierInfo`: LeaguePersistencePort에 솔로 랭크 전용 조회 메서드 추가
   - DuoService의 private 메서드 제거 → 각 도메인의 캡슐화된 메서드 호출로 대체

### 개선 가능 사항 (위반은 아님)
1. **ReadModel을 Java Record로 전환 고려** - 현재 Lombok 기반 불변 클래스이지만, CLAUDE.md에서 선호하는 Record 패턴으로 전환 가능
2. **`DuoPostListReadModel`에 `of()` 팩토리 메서드 부재** - QueryDSL 프로젝션으로 직접 생성되므로 현재 구조에서는 불필요하지만, 일관성 측면에서 검토 가능
3. **`DuoRequestTest` 테스트 보강** - 상태 전이(`accept`, `confirm`, `reject`, `cancel`)에 대한 테스트가 보강되면 좋음
