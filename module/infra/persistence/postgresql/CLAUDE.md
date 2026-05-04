# infra:persistence:postgresql

PostgreSQL 영속성 어댑터 (driven adapter). 도메인의 `*PersistencePort` 인터페이스를 JPA + QueryDSL + MapStruct 조합으로 구현한다. Flyway 마이그레이션도 이 모듈이 관리한다 (`lol-db-schema/` 를 srcDir로 마운트).

## Boundaries

- 허용: `core:lol-server-domain`, `core:enum`, Spring Data JPA, QueryDSL, MapStruct, Flyway, p6spy(개발 전용), H2(테스트 전용)
- 금지: 컨트롤러 코드, RestClient, 다른 인프라 모듈 import
- 도메인이 모르는 JPA 디테일 (`@Entity`, `@OneToMany`, `JpaRepository`) 은 모두 이 모듈 안에서 끝난다 — Adapter가 도메인 타입으로 번역해 반환한다

## Layout

각 도메인별 `repository/<domain>/` 하위:
- `entity/` — JPA `@Entity` (도메인과 다른 클래스)
- `repository/` 또는 도메인 폴더 직속 — `*JpaRepository extends JpaRepository`
- `dsl/` — `*RepositoryCustom` + `*RepositoryCustomImpl` (QueryDSL `JPAQueryFactory`)
- `mapper/` 또는 도메인 폴더 직속 — `@Mapper(componentModel = "spring")` MapStruct 인터페이스 (`toDomain`, `toEntity`)
- `adapter/` — `*PersistenceAdapter implements *PersistencePort` (`@Component`, JpaRepository + Custom + Mapper 조합)
- `dto/` — `*DTO` (QueryDSL projection 결과)

## Key Files

- `repository/duo/adapter/DuoPostPersistenceAdapter.java` — 표준 어댑터 reference
- `repository/duo/mapper/DuoPostMapper.java` — Lane/Status enum 변환을 `expression` 으로 처리하는 MapStruct 패턴
- `repository/duo/dsl/DuoPostRepositoryCustomImpl.java` — QueryDSL `Projections.fields` + `BooleanExpression` 동적 조건 reference
- `repository/match/adapter/MatchPersistenceAdapter.java` — `queryExecutor` (Virtual Thread) 로 병렬 쿼리 실행하는 무거운 어댑터
- `config/AsyncQueryConfig.java` — `queryExecutor` 빈 정의 (Virtual Thread + MDC 전파)
- `config/QueryDslConfig.java` — `JPAQueryFactory` 빈
- `src/test/java/.../repository/config/RepositoryTestBase.java` — 모든 JPA 테스트의 베이스 (`@DataJpaTest` + H2 + `QueryDslConfig` import)
- `lol-db-schema/` — Flyway 마이그레이션 srcDir (`build.gradle` 의 `sourceSets.main.resources.srcDir`)

## Common Modifications

- **새 영속화 도메인 추가**:
  1. `entity/XxxEntity.java` — JPA 매핑
  2. `repository/XxxJpaRepository.java extends JpaRepository<XxxEntity, ID>`
  3. `mapper/XxxMapper.java` — `@Mapper(componentModel = "spring")`, `toDomain` / `toEntity` (필요시 `expression` 으로 enum/VO 변환)
  4. `adapter/XxxPersistenceAdapter.java implements XxxPersistencePort` — 도메인 객체만 받고 도메인 객체만 반환
  5. Mapper 단위 테스트 필수 (`MapperImpl` + `ReflectionTestUtils.setField()` 또는 `Mapper.INSTANCE`)
- **동적 쿼리 추가**: `dsl/XxxRepositoryCustom` + `Impl` 에 `JPAQueryFactory` 사용. Slice 결과는 `SliceImpl`.
- **새 Flyway 마이그레이션**: `lol-db-schema/db/migration/V<n>__<desc>.sql` 추가.

## Failure Patterns / Gotchas

- ❌ Adapter 가 `XxxEntity` 를 반환 — 도메인이 엔티티를 알게 된다
  ✅ Adapter 는 항상 Mapper 로 도메인 타입 변환 후 반환
- ❌ MapStruct Mapper 추가 후 `*MapperTest` 미작성 — `@Mapping(ignore=true)`/`@AfterMapping` 회귀가 묻힘
  ✅ `componentModel = "spring"` 은 `new XxxMapperImpl()` + `ReflectionTestUtils.setField` 로 의존 Mapper 주입해 단위 테스트
- ❌ `updateEntityFromDomain` 에서 기존 컬렉션에 새 엔티티를 그냥 add — 중복 추가 발생
  ✅ 테스트로 컬렉션 사이즈 검증, `@AfterMapping` 으로 clear 후 add
- ❌ `MatchPersistenceAdapter` 에서 동기 쿼리 여러 개를 순차 실행
  ✅ `queryExecutor` (`AsyncQueryConfig`) + `CompletableFuture.supplyAsync` 로 병렬화 (이미 패턴이 있음)
- ❌ QueryDSL Q-class 가 안 잡힘 — annotationProcessor 미 동작
  ✅ `./gradlew clean build` 후 `build/generated/sources/annotationProcessor/...` 확인
- ❌ Adapter 에서 `OAuthProvider.RIOT.toString()` / `"RIOT"` 사용
  ✅ `OAuthProvider.RIOT.name()` (도메인 enum 그대로 사용 — 매직 스트링 금지 규칙은 어댑터에도 적용)

## Cross-Module Dependencies

- depends on: `core:lol-server-domain` (PersistencePort, 도메인 객체), `core:enum`, `support:logging`
- consumed by: `app:application` (런타임 빈 주입). 다른 인프라 모듈은 직접 의존 안 함
- `queryExecutor` 빈은 이 모듈이 정의 — Virtual Thread 기반, MDC 전파 포함

## Quick Commands

```bash
./gradlew :module:infra:persistence:postgresql:test            # JPA + Mapper + QueryDSL 테스트
./gradlew :module:infra:persistence:postgresql:compileJava     # QueryDSL Q-class 재생성
./gradlew :module:infra:persistence:postgresql:flywayMigrate   # Flyway 수동 적용 (필요 시)
```

## See Also

- [core:lol-server-domain](../../../core/lol-server-domain/CLAUDE.md) — 구현 대상 PersistencePort 의 출처
- [redis](../redis/CLAUDE.md) — 같은 도메인의 캐시 어댑터 (Cache vs DB 책임 분리 확인용)
- [clickhouse](../clickhouse/CLAUDE.md) — 분석 쿼리용 어댑터
- 테스트 명령: `./gradlew :module:infra:persistence:postgresql:test`
