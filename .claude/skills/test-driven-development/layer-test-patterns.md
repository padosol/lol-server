# Layer-Specific Test Patterns

## Overview

| 계층 | 핵심 특징 | 참조 테스트 |
|------|----------|------------|
| 도메인 서비스 | `@ExtendWith(MockitoExtension.class)`, Port Mock, `@InjectMocks` | `SummonerServiceTest` |
| 영속성 어댑터 (Mock) | `@BeforeEach` 수동 생성자 주입, Repository/Mapper Mock | `MatchPersistenceAdapterTest` |
| 영속성 어댑터 (DB) | `RepositoryTestBase` 상속, `@Autowired`, `entityManager.flush/clear` | `SummonerPersistenceAdapterTest` |
| MapStruct 매퍼 | `@ExtendWith` 없음, `INSTANCE` 직접 사용 | `MatchMapperTest` |
| RestDocs 컨트롤러 | `RestDocsSupport` 상속, `initController()` 오버라이드, `document()` | `SummonerControllerTest` |
| 클라이언트 어댑터 | `@ExtendWith(MockitoExtension.class)`, RestClient Mock, 수동 생성자 주입 | `SummonerClientAdapterTest` |

## Persistence Adapter Test (Mock 기반)

```java
@ExtendWith(MockitoExtension.class)
class SomePersistenceAdapterTest {

    @Mock
    private SomeRepositoryCustom someRepositoryCustom;

    @Mock
    private SomeJpaRepository someJpaRepository;

    @Mock
    private SomeMapper someMapper;

    private SomePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SomePersistenceAdapter(
                someRepositoryCustom,
                someJpaRepository,
                someMapper
        );
    }

    @DisplayName("ID로 조회하면 도메인 객체를 반환한다")
    @Test
    void findById_존재하는ID_도메인반환() {
        // given
        SomeEntity entity = createEntity("id-1");
        SomeDomain domain = createDomain("id-1");
        given(someJpaRepository.findById("id-1")).willReturn(Optional.of(entity));
        given(someMapper.toDomain(entity)).willReturn(domain);

        // when
        Optional<SomeDomain> result = adapter.findById("id-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("id-1");
        then(someJpaRepository).should().findById("id-1");
    }
}
```

**핵심:** `@InjectMocks` 대신 `@BeforeEach`에서 수동 생성자 주입. Mapper는 별도 테스트.

## Persistence Adapter Test (DB 기반)

```java
class SomePersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private SomeJpaRepository someJpaRepository;

    @Autowired
    private SomeMapper someMapper;

    @Autowired
    private EntityManager entityManager;

    private SomePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        SomeRepositoryCustom customRepo = new SomeRepositoryCustomImpl(queryFactory);
        adapter = new SomePersistenceAdapter(customRepo, someJpaRepository, someMapper);
    }

    @DisplayName("저장된 엔티티를 조회하면 도메인 객체를 반환한다")
    @Test
    void find_저장된엔티티_도메인반환() {
        // given
        someJpaRepository.save(createEntity("id-1"));
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<SomeDomain> result = adapter.findById("id-1");

        // then
        assertThat(result).isPresent();
    }
}
```

**핵심:** `RepositoryTestBase` 상속, `@Autowired` 실제 빈, `entityManager.flush/clear` 캐시 초기화.

## MapStruct Mapper Test

```java
class SomeMapperTest {

    private final SomeMapper mapper = SomeMapper.INSTANCE;

    @DisplayName("Entity를 Domain으로 변환한다")
    @Test
    void toDomain_정상엔티티_도메인반환() {
        // given
        SomeEntity entity = SomeEntity.builder()
                .id("id-1")
                .name("test")
                .build();

        // when
        SomeDomain result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("id-1");
        assertThat(result.getName()).isEqualTo("test");
    }

    @DisplayName("null Entity 변환 시 null을 반환한다")
    @Test
    void toDomain_null_null반환() {
        // when
        SomeDomain result = mapper.toDomain(null);

        // then
        assertThat(result).isNull();
    }
}
```

**핵심:** 기반 클래스 없음. `INSTANCE` 직접 사용. VO는 `ReflectionTestUtils.setField()`. 전체 필드 매핑 검증.

## RestDocs Controller Test

```java
@ExtendWith(MockitoExtension.class)
class SomeControllerTest extends RestDocsSupport {

    @Mock
    private SomeService someService;

    @InjectMocks
    private SomeController someController;

    @Override
    protected Object initController() {
        return someController;
    }

    @DisplayName("상세 조회 API")
    @Test
    void getDetail() throws Exception {
        // given
        SomeResponse response = SomeResponse.builder()
                .id("id-1")
                .name("test")
                .build();
        given(someService.getDetail("id-1")).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(
                get("/api/v1/some/{id}", "id-1"));

        // then
        result.andExpect(status().isOk())
                .andDo(document("some-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("결과"),
                                fieldWithPath("data.id").type(JsonFieldType.STRING).description("ID"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러")
                        )
                ));
    }
}
```

**핵심:** `RestDocsSupport` 상속, `initController()` 오버라이드, `document()`로 API 문서화, `responseFields` 완전성.

## Client Adapter Test

```java
@ExtendWith(MockitoExtension.class)
class SomeClientAdapterTest {

    @Mock
    private SomeRestClient someRestClient;

    @Mock
    private SomeClientMapper someClientMapper;

    private SomeClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SomeClientAdapter(someRestClient, someClientMapper);
    }

    @DisplayName("외부 API 조회 성공 시 도메인 객체를 반환한다")
    @Test
    void fetch_성공_도메인반환() {
        // given
        SomeVO vo = new SomeVO();
        ReflectionTestUtils.setField(vo, "id", "id-1");
        SomeDomain domain = createDomain("id-1");

        given(someRestClient.fetch("id-1")).willReturn(vo);
        given(someClientMapper.toDomain(vo)).willReturn(domain);

        // when
        Optional<SomeDomain> result = adapter.fetch("id-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("id-1");
        then(someRestClient).should().fetch("id-1");
    }

    @DisplayName("외부 API가 null 반환 시 빈 Optional을 반환한다")
    @Test
    void fetch_null응답_빈Optional() {
        // given
        given(someRestClient.fetch(anyString())).willReturn(null);

        // when
        Optional<SomeDomain> result = adapter.fetch("non-existent");

        // then
        assertThat(result).isEmpty();
    }
}
```

**핵심:** RestClient Mock, `@BeforeEach` 수동 생성자 주입, VO는 `ReflectionTestUtils.setField()`, null 응답 케이스 필수.

## Parameterized Test Patterns

### @CsvSource (여러 입출력 쌍)

```java
@DisplayName("티어 코드를 문자열로 변환한다")
@ParameterizedTest(name = "{0} -> {1}")
@CsvSource({
    "1000, IRON",
    "2000, BRONZE",
    "3000, SILVER",
    "4000, GOLD",
    "5000, PLATINUM"
})
void convertTierCode_코드_문자열변환(int code, String expected) {
    assertThat(TierConverter.toTierString(code)).isEqualTo(expected);
}
```

### @ValueSource (단순 값 리스트)

```java
@DisplayName("빈 문자열 입력을 거부한다")
@ParameterizedTest
@ValueSource(strings = {"", " ", "  "})
void validate_빈문자열_예외발생(String input) {
    assertThatThrownBy(() -> validator.validate(input))
            .isInstanceOf(CoreException.class);
}
```

### @MethodSource (복잡한 객체)

```java
@DisplayName("매치 데이터를 도메인으로 변환한다")
@ParameterizedTest
@MethodSource("provideMatchCases")
void toDomain_다양한매치_정상변환(MatchEntity entity, String expectedId) {
    SomeDomain result = mapper.toDomain(entity);
    assertThat(result.getMatchId()).isEqualTo(expectedId);
}

static Stream<Arguments> provideMatchCases() {
    return Stream.of(
        Arguments.of(createMatch("KR_001"), "KR_001"),
        Arguments.of(createMatch("KR_002"), "KR_002")
    );
}
```

### TDD에서의 활용법

RED 단계에서 일부 케이스로 시작 → GREEN 후 케이스 확장:

```java
// RED: 기본 케이스 하나로 시작
@CsvSource({"1000, IRON"})

// GREEN: 최소 구현으로 통과

// REFACTOR: 케이스 확장
@CsvSource({"1000, IRON", "2000, BRONZE", "3000, SILVER", "4000, GOLD"})
```

## Verification Checklist (계층별)

### Domain Service

- [ ] Port 인터페이스만 Mock (`@Mock SomePort`)
- [ ] 반환값 검증 (`assertThat(result).isEqualTo(...)`)
- [ ] `CoreException` + `ErrorType` 검증
- [ ] `@InjectMocks` 사용

### Persistence Adapter

- [ ] `@BeforeEach`에서 어댑터 수동 생성자 주입
- [ ] Mapper는 별도 테스트 클래스에서 검증
- [ ] DB 기반: `entityManager.flush/clear` 사용
- [ ] Mock 기반: Repository/Mapper 모두 Mock

### MapStruct Mapper

- [ ] `INSTANCE` 직접 사용 (Mock 없음)
- [ ] 전체 필드 매핑 확인 (누락 필드 없음)
- [ ] null 입력 케이스 검증
- [ ] 빈 리스트 케이스 검증
- [ ] VO 필드 설정: `ReflectionTestUtils.setField()` 사용

### RestDocs Controller

- [ ] `RestDocsSupport` 상속
- [ ] `initController()` 오버라이드
- [ ] `responseFields` 모든 필드 포함 (누락 시 RestDocs 에러)
- [ ] `preprocessRequest/Response(prettyPrint())` 적용
- [ ] `pathParameters` / `queryParameters` 문서화

### Client Adapter

- [ ] RestClient Mock
- [ ] `@BeforeEach`에서 어댑터 수동 생성자 주입
- [ ] null 응답 케이스 검증
- [ ] `ReflectionTestUtils.setField()`로 VO 필드 설정
