# DuoService / DuoRequestService 분리 계획

## Context

현재 `DuoService`가 DuoPost(게시글)와 DuoRequest(신청) 두 애그리게이트의 모든 유스케이스를 단일 클래스에서 구현하고 있음. 컨트롤러(`DuoPostController`, `DuoRequestController`)와 포트 인터페이스는 이미 분리되어 있으므로, 서비스 계층만 분리하면 됨. 목표는 단일 책임 원칙에 맞게 서비스를 분리하는 것.

## 설계 결정

### 1. 공유 헬퍼 메서드 처리 → `RiotAccountResolver` 추출

`extractRiotPuuid`와 `lookupTierInfo`는 `createDuoPost`과 `createDuoRequest` 양쪽에서 사용됨. `@Component` 클래스로 추출하여 양쪽 서비스에서 주입받아 사용.

### 2. 크로스 애그리게이트 의존성 → 직접 포트 주입

DuoRequest 메서드들이 DuoPost를 조회/수정하는 케이스 (소유자 검증, `confirmDuoRequest`의 `markMatched` 등)는 같은 바운디드 컨텍스트 내이므로 `DuoPostPersistencePort`를 `DuoRequestService`에 직접 주입. 마찬가지로 `DuoService.getDuoPost`이 요청 목록을 조회하는 건 `DuoRequestPersistencePort`를 유지.

## 변경 파일 목록

### 신규 생성

| 파일 | 설명 |
|------|------|
| `.../duo/application/RiotAccountResolver.java` | `extractRiotPuuid` + `lookupTierInfo` 공유 헬퍼 |
| `.../duo/application/DuoRequestService.java` | `DuoRequestUseCase` + `DuoRequestQueryUseCase` 구현 |
| `.../duo/application/RiotAccountResolverTest.java` | RiotAccountResolver 단위 테스트 |
| `.../duo/application/DuoRequestServiceTest.java` | DuoRequest 메서드 단위 테스트 (DuoServiceTest에서 이동) |

### 수정

| 파일 | 설명 |
|------|------|
| `.../duo/application/DuoService.java` | DuoRequest 관련 코드 제거, `RiotAccountResolver` 주입 |
| `.../duo/application/DuoServiceTest.java` | DuoRequest 테스트 제거, `RiotAccountResolver` Mock 적용 |

### 변경 없음 (검증만)

- `DuoPostController.java`, `DuoRequestController.java` - UseCase 인터페이스로 주입하므로 변경 불필요
- 포트 인터페이스 (`DuoPostUseCase`, `DuoRequestUseCase` 등) - 변경 없음

## 상세 구현

### Step 1: `RiotAccountResolver` 생성

```java
@Component
@RequiredArgsConstructor
public class RiotAccountResolver {
    private final MemberPersistencePort memberPersistencePort;
    private final LeaguePersistencePort leaguePersistencePort;

    public String extractRiotPuuid(Long memberId) { ... }
    public TierInfo lookupTierInfo(String puuid) { ... }
}
```

### Step 2: `DuoRequestService` 생성

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DuoRequestService implements DuoRequestUseCase, DuoRequestQueryUseCase {
    private final DuoRequestPersistencePort duoRequestPersistencePort;
    private final DuoPostPersistencePort duoPostPersistencePort;
    private final SummonerPersistencePort summonerPersistencePort;
    private final RiotAccountResolver riotAccountResolver;
}
```

이동할 메서드: `createDuoRequest`, `acceptDuoRequest`, `confirmDuoRequest`, `rejectDuoRequest`, `cancelDuoRequest`, `getDuoRequestsForPost`, `getMyDuoRequests`

헬퍼 호출 변경: `extractRiotPuuid(memberId)` → `riotAccountResolver.extractRiotPuuid(memberId)`

### Step 3: `DuoService` 정리

- `implements DuoPostUseCase, DuoPostQueryUseCase`만 유지
- 의존성: `DuoPostPersistencePort`, `DuoRequestPersistencePort` (getDuoPost용), `RiotAccountResolver`
- `MemberPersistencePort`, `LeaguePersistencePort`, `SummonerPersistencePort` 제거
- 7개 DuoRequest 메서드 + 2개 private 헬퍼 제거

### Step 4: 테스트 분리

**DuoRequestServiceTest** - DuoServiceTest에서 아래 @Nested 클래스 이동:
- `CreateDuoRequest`, `AcceptDuoRequest`, `ConfirmDuoRequest`, `RejectDuoRequest`, `CancelDuoRequest`, `GetDuoRequestsForPost`
- Mock 변경: `memberPersistencePort`/`leaguePersistencePort` → `riotAccountResolver`

**DuoServiceTest** 수정:
- DuoRequest 관련 @Nested 클래스 제거
- `createDuoPost` 테스트의 Mock을 `riotAccountResolver`로 변경

**RiotAccountResolverTest** - 추출된 헬퍼 로직 테스트

## 검증 방법

```bash
# 전체 테스트
./gradlew test

# Duo 도메인 테스트만
./gradlew :module:core:lol-server-domain:test --tests "com.example.lolserver.domain.duo.*"

# 빌드 확인
./gradlew build
```
