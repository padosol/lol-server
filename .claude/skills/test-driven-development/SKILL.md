---
name: test-driven-development
description: Use when implementing any feature or bugfix, before writing implementation code
---

# TDD (테스트 주도 개발)

## 개요

테스트를 먼저 작성하고, 실패를 확인하고, 통과할 최소 코드를 작성한다.

**핵심 원칙:** 테스트가 실패하는 것을 보지 않았다면, 그 테스트가 올바른 것을 검증하는지 알 수 없다.

**규칙의 문구를 어기는 것은 규칙의 정신을 어기는 것이다.**

## 적용 시점

**항상 적용:**
- 새 기능 구현
- 버그 수정
- 리팩토링
- 동작 변경

**예외 (반드시 사용자에게 확인):**
- 일회성 프로토타입
- 생성된 코드
- 설정 파일

"이번만 TDD 건너뛰자"는 생각이 들면 멈춰라. 그것은 합리화다.

## 철칙

```
실패하는 테스트 없이 프로덕션 코드를 작성하지 않는다
```

테스트 전에 코드를 작성했다면 삭제하고 처음부터 시작한다.

**예외 없음:**
- "참고용"으로 남기지 않는다
- 테스트 작성하면서 "적용"하지 않는다
- 보지도 않는다
- 삭제는 삭제다

테스트로부터 새로 구현한다.

## Red-Green-Refactor

### RED - 실패하는 테스트 작성

원하는 동작을 보여주는 최소한의 테스트 하나를 작성한다.

```java
@DisplayName("실패한 작업을 3회 재시도한다")
@Test
void retryOperation_실패시_3회재시도() {
    // given
    AtomicInteger attempts = new AtomicInteger(0);

    // when
    String result = retryService.retryOperation(() -> {
        if (attempts.incrementAndGet() < 3) {
            throw new RuntimeException("fail");
        }
        return "success";
    });

    // then
    assertThat(result).isEqualTo("success");
    assertThat(attempts.get()).isEqualTo(3);
}
```
명확한 이름, 실제 동작 테스트, 한 가지만 검증

**요구사항:**
- 하나의 동작만 테스트
- 명확한 이름 (한글 `@DisplayName` 권장)
- 실제 코드 사용 (Mock은 불가피한 경우만)

**RED 검증 — 반드시 실행:**

```bash
./gradlew test --tests RetryServiceTest
```

확인 사항:
- 테스트가 실패한다 (에러가 아님)
- 실패 메시지가 예상과 일치한다
- 기능 미구현으로 실패한다 (오타가 아님)

테스트가 통과하면? 이미 존재하는 동작을 테스트하고 있다. 테스트를 수정한다.
테스트가 에러나면? 에러를 수정하고 올바르게 실패할 때까지 재실행한다.

### GREEN - 최소 코드 작성

테스트를 통과시키는 가장 단순한 코드를 작성한다.

```java
public <T> T retryOperation(Supplier<T> operation) {
    for (int i = 0; i < 3; i++) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            if (i == 2) throw e;
        }
    }
    throw new IllegalStateException("unreachable");
}
```
통과에 필요한 만큼만 작성

기능을 추가하거나, 다른 코드를 리팩토링하거나, 테스트 범위를 넘어 "개선"하지 않는다.

**GREEN 검증 — 반드시 실행:**

```bash
./gradlew test --tests RetryServiceTest
```

확인 사항:
- 테스트가 통과한다
- 다른 테스트도 여전히 통과한다
- 출력이 깨끗하다 (에러, 경고 없음)

테스트가 실패하면? 코드를 수정한다 (테스트가 아님).
다른 테스트가 실패하면? 지금 수정한다.

### REFACTOR - 정리

GREEN 이후에만 수행:
- 중복 제거
- 이름 개선
- 헬퍼 추출

테스트는 계속 GREEN 상태를 유지한다. 동작을 추가하지 않는다.

### 반복

다음 기능을 위한 다음 실패 테스트를 작성한다.

## 좋은 테스트

| 품질 | 좋은 예 | 나쁜 예 |
|------|---------|---------|
| **최소** | 한 가지만. 이름에 "그리고"가 있으면 분리 | `@DisplayName("이메일과 도메인과 공백을 검증한다")` |
| **명확** | 이름이 동작을 설명 | `@Test void test1()` |
| **의도 표현** | 원하는 API를 보여줌 | 코드가 뭘 해야 하는지 불분명 |

## 흔한 합리화

| 핑계 | 현실 |
|------|------|
| "테스트하기엔 너무 단순" | 단순한 코드도 깨진다. 테스트 30초면 된다. |
| "나중에 테스트 추가하지" | 즉시 통과하는 테스트는 아무것도 증명 못한다. |
| "테스트 후작성도 같은 효과" | 후작성 = "이게 뭘 하지?" 선작성 = "이게 뭘 해야 하지?" |
| "이미 수동 테스트 했는데" | 즉흥적 ≠ 체계적. 기록 없고, 재실행 불가. |
| "X시간 삭제는 낭비" | 매몰 비용 오류. 검증 안 된 코드 유지가 기술 부채. |
| "참고용으로 남기고 테스트부터" | 적용하게 된다. 그건 후작성이다. 삭제는 삭제. |
| "먼저 탐색이 필요해" | 좋다. 탐색 코드 버리고 TDD로 시작. |
| "테스트 어려움 = 설계 불명확" | 테스트에 귀 기울여라. 테스트 어려움 = 사용 어려움. |
| "TDD는 느려" | TDD가 디버깅보다 빠르다. 실용적 = 테스트 선작성. |
| "수동 테스트가 더 빨라" | 수동은 엣지 케이스를 증명 못한다. 변경마다 다시 테스트해야 한다. |
| "기존 코드에 테스트 없는데" | 개선하는 거다. 기존 코드에 테스트를 추가해라. |

## 테스트 클래스 구조 (권장 패턴)

```java
@ExtendWith(MockitoExtension.class)
class SomeServiceTest {

    @Mock
    private SomePort somePort;  // Port 인터페이스 Mock

    @InjectMocks
    private SomeService someService;

    // ========== 메서드명 테스트 ==========

    @DisplayName("조건이 참이면 기대 결과를 반환한다")
    @Test
    void methodName_조건_기대결과() {
        // given
        SomeInput input = new SomeInput("value");
        given(somePort.findSomething("value")).willReturn(Optional.of(expected));

        // when
        SomeResult result = someService.methodName(input);

        // then
        assertThat(result.getValue()).isEqualTo("expected");
        then(somePort).should().findSomething("value");
    }

    @DisplayName("조건이 거짓이면 예외를 던진다")
    @Test
    void methodName_조건거짓_예외발생() {
        // given
        SomeInput input = new SomeInput("invalid");
        given(somePort.findSomething("invalid")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> someService.methodName(input))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
    }
}
```

## 계층별 테스트 패턴

| 계층 | 기반 클래스/어노테이션 | DI 방식 | 핵심 포인트 |
|------|----------------------|---------|------------|
| 도메인 서비스 | `@ExtendWith(MockitoExtension.class)` | `@InjectMocks` | Port 인터페이스만 Mock |
| 영속성 어댑터 (Mock) | `@ExtendWith(MockitoExtension.class)` | `@BeforeEach` 수동 주입 | Mapper 별도 테스트 |
| 영속성 어댑터 (DB) | `RepositoryTestBase` 상속 | `@Autowired` | `entityManager.flush/clear` |
| MapStruct 매퍼 | 없음 | `INSTANCE` 직접 사용 | 전체 필드 + null 검증 |
| RestDocs 컨트롤러 | `RestDocsSupport` 상속 | `@InjectMocks` | `initController()` + `document()` |
| 클라이언트 어댑터 | `@ExtendWith(MockitoExtension.class)` | `@BeforeEach` 수동 주입 | null 응답 + `ReflectionTestUtils` |

코드 예시, Parameterized Test 패턴, 계층별 체크리스트는 @layer-test-patterns.md 참조.

## 검증 체크리스트

- [ ] 모든 새 함수/메서드에 테스트가 있다
- [ ] 각 테스트가 실패하는 것을 구현 전에 확인했다
- [ ] 각 테스트가 예상된 이유로 실패했다 (기능 미구현, 오타 아님)
- [ ] 각 테스트를 통과시키는 최소 코드를 작성했다
- [ ] 모든 테스트가 통과한다
- [ ] 출력이 깨끗하다 (에러, 경고 없음)
- [ ] 테스트가 실제 코드를 사용한다 (Mock은 불가피한 경우만)
- [ ] 엣지 케이스와 에러가 커버되었다
- [ ] 계층별 체크리스트 확인 (@layer-test-patterns.md)

모든 항목을 체크할 수 없다면 TDD를 건너뛴 것이다. 처음부터 다시 시작한다.

## 막혔을 때

| 문제 | 해결 |
|------|------|
| 테스트 방법을 모르겠다 | 원하는 API를 작성한다. assertion부터 작성한다. 사용자에게 물어본다. |
| 테스트가 너무 복잡하다 | 설계가 너무 복잡하다. 인터페이스를 단순화한다. |
| 모든 걸 Mock해야 한다 | 코드 결합도가 높다. Port 인터페이스로 의존성 주입을 사용한다. |
| 테스트 셋업이 거대하다 | 헬퍼를 추출하거나 `@BeforeEach`를 사용한다. 그래도 복잡하면 설계를 단순화한다. |
| 버그를 발견했다 | 버그를 재현하는 실패 테스트를 작성한다. TDD 사이클을 따른다. 테스트 없이 수정하지 않는다. |

## 테스트 안티패턴

Mock이나 테스트 유틸리티를 추가할 때 @testing-anti-patterns.md 를 읽고 흔한 함정을 피한다:
- Mock 동작을 테스트하는 것 (실제 동작 대신)
- 프로덕션 클래스에 테스트 전용 메서드 추가
- 의존성을 이해하지 않고 Mock 사용

## 최종 규칙

```
프로덕션 코드 → 테스트가 존재하고 먼저 실패했어야 한다
그렇지 않으면 → TDD가 아니다
```

사용자의 허락 없이 예외는 없다.
