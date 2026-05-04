# core:lol-server-domain

헥사고날 아키텍처의 **도메인 코어**. 비즈니스 로직, 애플리케이션 서비스, in/out 포트만 거주한다. Spring Data, JPA, RestClient 같은 인프라 어댑터는 절대 들어오면 안 된다.

## Boundaries

- 허용 의존성: `core:enum`, `support:logging`, `org.springframework:spring-context`/`spring-tx`(트랜잭션 어노테이션만), Lombok, slf4j, jackson-annotations
- 금지: `infra:*` 모든 모듈, `@Entity`, `JpaRepository`, `RestClient`, `RabbitTemplate`, `RedisTemplate`
- `build.gradle` 주석에 적힌 "TEMPORARY" 의존성 (slf4j, jackson-annotations) 은 점진적으로 제거 대상이지만 신규 코드에서 직접 활용은 피할 것

## Subdomain layout

각 서브도메인 (`domain/<name>/`) 은 동일 골격:
- `domain/` — 순수 비즈니스 객체 (Lombok `@Builder` + `@AllArgsConstructor(access = PRIVATE)`, 정적 팩토리 `create*`)
- `domain/vo/` — 값 객체 / enum (`Lane`, `DuoPostStatus`, `OAuthProvider` 등)
- `application/<XxxService>.java` — `@Service @RequiredArgsConstructor @Transactional(readOnly = true)`, 변경 메서드에만 `@Transactional`
- `application/port/in/` — UseCase 인터페이스 (컨트롤러 진입점)
- `application/port/out/` — PersistencePort/Client/MessagePort (인프라 어댑터가 구현)
- `application/command/` 또는 `application/dto/` — 입력 DTO (`@Builder @Getter @NoArgsConstructor @AllArgsConstructor`)
- `application/model/` — `*ReadModel` 출력 DTO (불변, `Builder` 또는 record, `of(domain)` 정적 팩토리 필수)

## Key Files

- `domain/duo/domain/DuoPost.java` — 도메인 객체 + guard 메서드 패턴의 reference (`validateOwner`, `validateActive`, `markDeleted`)
- `domain/duo/application/DuoService.java` — 표준 애플리케이션 서비스 형태 (port in 구현 + port out 호출 + ReadModel.of 변환)
- `domain/duo/application/model/DuoPostReadModel.java` — `of(DuoPost)` 정적 팩토리의 정석 예
- `support/error/CoreException.java`, `support/error/ErrorType.java` — 모든 비즈니스 예외의 공통 진입점, HTTP 상태 매핑 enum
- `support/PaginationRequest.java`, `support/SliceResult.java`, `support/PageResult.java` — Spring `Page` 대신 사용하는 도메인 페이징 타입

## Common Modifications

- **새 서브도메인 추가**: `domain/<name>/` 하위에 `domain/`, `application/`, `application/port/in/`, `application/port/out/`, `application/model/`, `application/command/`(필요 시) 골격을 만든다. 인프라가 어댑터를 구현해야 동작한다.
- **새 비즈니스 규칙**: 도메인 객체에 메서드를 추가하라. Service에 if/throw로 비즈니스 규칙을 흩지 마라.
- **새 ReadModel 필드 추가**: ReadModel 클래스 + `of()` 팩토리만 수정. Service의 인라인 빌더 변환은 금지.
- **새 ErrorType**: `support/error/ErrorType.java` enum에 `(HttpStatus, code, message)` 추가 후 도메인에서 `throw new CoreException(ErrorType.XXX)`.

## Failure Patterns / Gotchas

- ❌ `if (!duoPost.isOwner(memberId)) throw new CoreException(...)` (서비스에서 boolean + 예외)
  ✅ `duoPost.validateOwner(memberId)` — 도메인이 직접 던진다. `is*` 는 분기용, `validate*` 는 불변식 강제용.
- ❌ Service에서 `DuoPostReadModel.builder()...build()` 인라인 변환
  ✅ `DuoPostReadModel.of(duoPost)` — 변환은 ReadModel 책임.
- ❌ `@Setter` 로 도메인 상태 변경 / public 생성자
  ✅ Builder + `AccessLevel.PRIVATE` 생성자 + `create*()`/도메인 메서드.
- ❌ `"RIOT"`, `"RANKED_SOLO_5x5"`, `420` 같은 매직 스트링/숫자
  ✅ `OAuthProvider.RIOT.name()`, `QueueType.RANKED_SOLO_5x5.name()`, `QueueType.RANKED_SOLO_5x5.getQueueId()`. 도메인 VO enum (`DuoPostStatus`, `DuoRequestStatus` 등) 동일.
- ❌ 도메인에서 `@Entity` import / `JpaRepository` import — 빌드는 통과해도 헥사고날 경계 위반.

## Cross-Module Dependencies

- depends on: `core:enum`, `support:logging`
- consumed by (어댑터로): `infra:api`, `infra:persistence:postgresql`, `infra:persistence:redis`, `infra:persistence:clickhouse`, `infra:client:lol-repository`, `infra:client:oauth`, `infra:message:rabbitmq`, `infra:message:kafka`
- 의존 방향은 항상 `infra → core` 단방향. 역방향 import 가 생기면 즉시 거절.

## See Also

- [Root CLAUDE.md](../../../CLAUDE.md) — 전체 컴퍼스
- [infra/api/CLAUDE.md](../../infra/api/CLAUDE.md) — UseCase 호출자
- [infra/persistence/postgresql/CLAUDE.md](../../infra/persistence/postgresql/CLAUDE.md) — out port 구현 (Adapter + MapStruct Mapper)
- `core:enum` (`module/core/enum/src/main/java/...`) — `QueueType`, `Tier`, `Platform` 등 매직 스트링 대체용 enum
