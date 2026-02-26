# Testing Anti-Patterns

**Load this reference when:** writing or changing tests, adding mocks, or tempted to add test-only methods to production code.

## Overview

Tests must verify real behavior, not mock behavior. Mocks are a means to isolate, not the thing being tested.

**Core principle:** Test what the code does, not what the mocks do.

**Following strict TDD prevents these anti-patterns.**

## The Iron Laws

```
1. NEVER test mock behavior
2. NEVER add test-only methods to production classes
3. NEVER mock without understanding dependencies
```

## Anti-Pattern 1: Testing Mock Behavior

**The violation:**
```java
// ❌ BAD: Testing that the mock exists
@Test
void getSummoner_소환사반환() {
    // given
    given(summonerPort.findById("puuid")).willReturn(Optional.of(mockSummoner));

    // when
    summonerService.getSummoner("puuid");

    // then
    then(summonerPort).should().findById("puuid");  // Mock 호출만 검증
    // 실제 반환값 검증 없음!
}
```

**Why this is wrong:**
- You're verifying the mock works, not that the service works
- Test passes when mock is called, but doesn't verify actual behavior
- Tells you nothing about real behavior

**your human partner's correction:** "Are we testing the behavior of a mock?"

**The fix:**
```java
// ✅ GOOD: Test actual behavior and return values
@DisplayName("puuid로 소환사를 조회하면 소환사 응답을 반환한다")
@Test
void getSummoner_puuid존재_소환사응답반환() {
    // given
    Summoner summoner = createSummoner("puuid-123", "TestPlayer", "KR1");
    given(summonerPort.findById("puuid-123")).willReturn(Optional.of(summoner));

    // when
    SummonerResponse result = summonerService.getSummoner("puuid-123");

    // then
    assertThat(result.getPuuid()).isEqualTo("puuid-123");
    assertThat(result.getGameName()).isEqualTo("TestPlayer");
    then(summonerPort).should().findById("puuid-123");
}
```

### Gate Function

```
BEFORE asserting on any mock element:
  Ask: "Am I testing real component behavior or just mock existence?"

  IF testing mock existence:
    STOP - Add assertions on actual return values

  Test real behavior instead
```

## Anti-Pattern 2: Test-Only Methods in Production

**The violation:**
```java
// ❌ BAD: destroyForTest() only used in tests
public class Session {
    public void destroyForTest() {  // Looks like production API!
        this.workspaceManager.destroyWorkspace(this.id);
        // ... cleanup
    }
}

// In tests
@AfterEach
void tearDown() {
    session.destroyForTest();
}
```

**Why this is wrong:**
- Production class polluted with test-only code
- Dangerous if accidentally called in production
- Violates YAGNI and separation of concerns
- Confuses object lifecycle with entity lifecycle

**The fix:**
```java
// ✅ GOOD: Test utilities handle test cleanup
// Session has no destroyForTest() - it's stateless in production

// In test-support package
public class SessionTestHelper {
    public static void cleanupSession(Session session, WorkspaceManager workspaceManager) {
        WorkspaceInfo workspace = session.getWorkspaceInfo();
        if (workspace != null) {
            workspaceManager.destroyWorkspace(workspace.getId());
        }
    }
}

// In tests
@AfterEach
void tearDown() {
    SessionTestHelper.cleanupSession(session, workspaceManager);
}
```

### Gate Function

```
BEFORE adding any method to production class:
  Ask: "Is this only used by tests?"

  IF yes:
    STOP - Don't add it
    Put it in test utilities instead

  Ask: "Does this class own this resource's lifecycle?"

  IF no:
    STOP - Wrong class for this method
```

## Anti-Pattern 3: Mocking Without Understanding

**The violation:**
```java
// ❌ BAD: Mock breaks test logic
@Test
void addServer_중복서버_예외발생() {
    // Mock prevents config write that test depends on!
    given(toolCatalog.discoverAndCacheTools())
        .willReturn(Optional.empty());

    serverManager.addServer(config);
    serverManager.addServer(config);  // Should throw - but won't!
}
```

**Why this is wrong:**
- Mocked method had side effect test depended on (writing config)
- Over-mocking to "be safe" breaks actual behavior
- Test passes for wrong reason or fails mysteriously

**The fix:**
```java
// ✅ GOOD: Mock at correct level
@Test
void addServer_중복서버_예외발생() {
    // given - Mock only the slow external operation
    given(mcpServerConnection.connect(any())).willReturn(mockConnection);

    // when
    serverManager.addServer(config);  // Config written

    // then
    assertThatThrownBy(() -> serverManager.addServer(config))
        .isInstanceOf(DuplicateServerException.class);  // Duplicate detected ✓
}
```

### Gate Function

```
BEFORE mocking any method:
  STOP - Don't mock yet

  1. Ask: "What side effects does the real method have?"
  2. Ask: "Does this test depend on any of those side effects?"
  3. Ask: "Do I fully understand what this test needs?"

  IF depends on side effects:
    Mock at lower level (the actual slow/external operation)
    OR use test doubles that preserve necessary behavior
    NOT the high-level method the test depends on

  IF unsure what test depends on:
    Run test with real implementation FIRST
    Observe what actually needs to happen
    THEN add minimal mocking at the right level

  Red flags:
    - "I'll mock this to be safe"
    - "This might be slow, better mock it"
    - Mocking without understanding the dependency chain
```

## Anti-Pattern 4: Incomplete Mocks

**The violation:**
```java
// ❌ BAD: Partial mock - only fields you think you need
SummonerResponse mockResponse = SummonerResponse.builder()
    .puuid("puuid-123")
    .gameName("TestPlayer")
    // Missing: tagLine, profileIconId, summonerLevel that downstream code uses
    .build();

// Later: breaks when code accesses response.getTagLine()
```

**Why this is wrong:**
- **Partial mocks hide structural assumptions** - You only mocked fields you know about
- **Downstream code may depend on fields you didn't include** - Silent failures
- **Tests pass but integration fails** - Mock incomplete, real API complete
- **False confidence** - Test proves nothing about real behavior

**The Iron Rule:** Mock the COMPLETE data structure as it exists in reality, not just fields your immediate test uses.

**The fix:**
```java
// ✅ GOOD: Mirror real API completeness
Summoner testSummoner = new Summoner(
    "puuid-123",
    100L,                    // profileIconId
    150,                     // summonerLevel
    "TestPlayer",            // gameName
    "KR1",                   // tagLine
    "kr",                    // region
    "testplayer",            // normalizedName
    LocalDateTime.now(),     // revisionDate
    LocalDateTime.now(),     // clickDate
    List.of()                // leagueSummoners
);
```

### Gate Function

```
BEFORE creating mock responses:
  Check: "What fields does the real API response contain?"

  Actions:
    1. Examine actual API response from docs/examples
    2. Include ALL fields system might consume downstream
    3. Verify mock matches real response schema completely

  Critical:
    If you're creating a mock, you must understand the ENTIRE structure
    Partial mocks fail silently when code depends on omitted fields

  If uncertain: Include all documented fields
```

## Anti-Pattern 5: Integration Tests as Afterthought

**The violation:**
```
✅ Implementation complete
❌ No tests written
"Ready for testing"
```

**Why this is wrong:**
- Testing is part of implementation, not optional follow-up
- TDD would have caught this
- Can't claim complete without tests

**The fix:**
```
TDD cycle:
1. Write failing test
2. Implement to pass
3. Refactor
4. THEN claim complete
```

## When Mocks Become Too Complex

**Warning signs:**
- Mock setup longer than test logic
- Mocking everything to make test pass
- Mocks missing methods real components have
- Test breaks when mock changes

**your human partner's question:** "Do we need to be using a mock here?"

**Consider:** Integration tests with real components often simpler than complex mocks

## Port Interface Mocking (헥사고날 아키텍처)

In hexagonal architecture, mock Port interfaces, not implementations:

```java
// ✅ GOOD: Mock the Port interface
@Mock
private SummonerClientPort summonerClientPort;

@Mock
private SummonerPersistencePort summonerPersistencePort;

@InjectMocks
private SummonerService summonerService;

@Test
void getSummoner_DB존재_소환사응답반환() {
    // given
    given(summonerPersistencePort.getSummoner("Player", "KR1", "kr"))
        .willReturn(Optional.of(summoner));

    // when
    SummonerResponse result = summonerService.getSummoner(gameName, "kr");

    // then
    assertThat(result.getGameName()).isEqualTo("Player");
    then(summonerClientPort).should(never()).getSummoner(any(), any(), any());
}
```

```java
// ❌ BAD: Mock concrete implementation
@Mock
private SummonerJpaRepository summonerJpaRepository;  // Don't mock implementations
```

## TDD Prevents These Anti-Patterns

**Why TDD helps:**
1. **Write test first** → Forces you to think about what you're actually testing
2. **Watch it fail** → Confirms test tests real behavior, not mocks
3. **Minimal implementation** → No test-only methods creep in
4. **Real dependencies** → You see what the test actually needs before mocking

**If you're testing mock behavior, you violated TDD** - you added mocks without watching test fail against real code first.

## Quick Reference

| Anti-Pattern | Fix |
|--------------|-----|
| Assert only on mock calls | Add assertions on actual return values |
| Test-only methods in production | Move to test utilities |
| Mock without understanding | Understand dependencies first, mock minimally |
| Incomplete mocks | Mirror real API completely |
| Tests as afterthought | TDD - tests first |
| Over-complex mocks | Consider integration tests |
| Mock implementations | Mock Port interfaces instead |

## Red Flags

- Assertions only verify `then(mock).should()` calls
- Methods only called in test files
- Mock setup is >50% of test
- Test fails when you remove mock
- Can't explain why mock is needed
- Mocking "just to be safe"
- Mocking concrete implementations instead of Port interfaces

## Anti-Pattern 6: Layer Boundary Violation Mock

**The violation - 도메인 서비스에서 JpaRepository 직접 Mock:**
```java
// ❌ BAD: 도메인 서비스 테스트에서 인프라 구현체를 직접 Mock
@ExtendWith(MockitoExtension.class)
class SummonerServiceTest {

    @Mock
    private SummonerJpaRepository summonerJpaRepository;  // Port가 아닌 구현체!

    @InjectMocks
    private SummonerService summonerService;

    @Test
    void getSummoner_소환사조회() {
        given(summonerJpaRepository.findByPuuid("puuid")).willReturn(entity);  // Entity 직접 반환!
        // ...
    }
}
```

**The violation - 영속성 어댑터에서 다른 어댑터 Mock:**
```java
// ❌ BAD: 영속성 어댑터 테스트에서 클라이언트 어댑터 Mock
@ExtendWith(MockitoExtension.class)
class SummonerPersistenceAdapterTest {

    @Mock
    private SummonerClientAdapter summonerClientAdapter;  // 다른 계층 어댑터!

    // ...
}
```

**Why this is wrong:**
- **헥사고날 아키텍처 위반** - 도메인이 인프라에 의존하면 안 됨
- **테스트가 구현 세부사항에 결합** - JPA 구현체가 바뀌면 도메인 테스트도 깨짐
- **어댑터 간 의존성 생성** - 각 어댑터는 독립적이어야 함
- **Port 인터페이스 존재 의미 상실** - 추상화 경계 무시

**The fix:**
```java
// ✅ GOOD: 도메인 서비스는 Port 인터페이스만 Mock
@ExtendWith(MockitoExtension.class)
class SummonerServiceTest {

    @Mock
    private SummonerPersistencePort summonerPersistencePort;  // Port 인터페이스

    @Mock
    private SummonerClientPort summonerClientPort;  // Port 인터페이스

    @InjectMocks
    private SummonerService summonerService;

    @Test
    void getSummoner_소환사조회() {
        given(summonerPersistencePort.getSummoner("Player", "KR1", "kr"))
                .willReturn(Optional.of(summoner));  // 도메인 객체 반환
        // ...
    }
}
```

```java
// ✅ GOOD: 영속성 어댑터는 자신의 Repository/Mapper만 Mock
@ExtendWith(MockitoExtension.class)
class SummonerPersistenceAdapterTest {

    @Mock
    private SummonerJpaRepository summonerJpaRepository;  // 같은 계층

    @Mock
    private SummonerMapper summonerMapper;  // 같은 계층

    // ...
}
```

### Gate Function

```
BEFORE mocking any dependency in a test:
  Ask: "Does this dependency belong to the same architectural layer?"

  IF domain service test:
    ONLY mock Port interfaces (SomePersistencePort, SomeClientPort)
    NEVER mock JpaRepository, RestClient, or Adapter implementations

  IF persistence adapter test:
    ONLY mock same-layer dependencies (Repository, Mapper)
    NEVER mock other adapters or domain services

  IF client adapter test:
    ONLY mock RestClient, ClientMapper
    NEVER mock persistence or domain components

  Rule: Each test mocks ONLY its own layer's dependencies
```

## The Bottom Line

**Mocks are tools to isolate, not things to test.**

If TDD reveals you're testing mock behavior, you've gone wrong.

Fix: Test real behavior or question why you're mocking at all.
