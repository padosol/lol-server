# ADR 0003: 적용은 lol-repository, lol-server 는 검증 — local 만 예외

## 상태
Accepted (2026-08-31, 2026-10-03 개정)

> 개정 이력: 최초 결정은 "lol-server 도 운영에서 마이그레이션을 적용한다"였다. DB 에 쓰는
> 주체를 하나로 유지하자는 판단에 따라 운영은 **검증만** 하도록 바꿨다. local 은 그대로
> 적용한다.

## 컨텍스트

마이그레이션은 `lol-repository` 가 단독으로 적용해 왔다. `lol-server` 는
`spring.flyway.enabled: false` + `ddl-auto: validate` 로 엔티티↔테이블만 검증했다.

그 대가가 로컬 개발에 그대로 걸렸다. 빈 DB 에서 `lol-server` 만 띄우면 스키마가 없어
`validate` 가 실패한다. `postgresql-local.yml` 주석이 "lol-repository 를 먼저 띄울 것"
이라고 적어 두고 있었다 — 문서로 우회한 구조적 불편이다.

두 리포는 **같은 `lol-db-schema` 서브모듈**을 싣는다. 즉 적용할 SQL 이 애초에 동일하다.

## 결정

**DB 에 쓰는 주체는 `lol-repository` 하나로 유지한다.** `lol-server` 는 운영에서
마이그레이션을 적용하지 않고 **검증**만 한다. local 에서는 적용한다.

- `FlywayValidateOnlyConfig` 가 `@Profile("prod")` 로 `FlywayMigrationStrategy` 빈을
  등록해 기동 시 `migrate()` 대신 `validate()` 를 돌린다. Boot 오토컨피그는 기본적으로
  `migrate()` 를 돌리고 그걸 끄는 프로퍼티는 없으므로 전략 빈이 유일한 방법이다.
- local 에는 그 빈이 없어 기본 전략(`migrate()`)이 그대로 돈다. 빈 DB 에 `lol-server`
  만 띄워도 스키마가 만들어진다 — 컨텍스트의 불편이 풀리는 지점이다.
- `processResources` 가 `lol-db-schema/db/migration` 을 `db/migration` 으로 복사한다.
  **검증에도 비교 대상 파일이 필요하므로** 이 패키징은 운영에서도 그대로 쓰인다.
- `flyway-core` 는 `implementation`(코드가 `Flyway` 타입을 참조한다),
  `flyway-database-postgresql` 은 `runtimeOnly`. 후자를 빼면 Flyway 10 부터 DB 별
  지원이 분리돼 `No database found to handle jdbc:postgresql://...` 로 기동이 죽는다.
- `ddl-auto: validate` 는 **유지**한다. Flyway 검증과 보는 것이 다르다 — 아래 참고.

## 두 검증은 대체 관계가 아니다

- **Flyway `validate()`** — *마이그레이션 이력*을 본다. 체크섬 불일치(같은 번호 다른 내용),
  DB 에 적용됐는데 내 빌드에 없는 마이그레이션(missing)을 잡는다.
- **`ddl-auto: validate`** — *엔티티와 실제 테이블*을 본다. 매핑된 컬럼이 없거나 타입이
  다르면 잡는다.

하나가 다른 하나를 덮지 못하므로 둘 다 켠다.

## 대가 — 배포 순서가 묶인다

**빌드에만 있고 DB 에 아직 적용되지 않은 마이그레이션(pending)이 있으면 `validate()` 가
실패한다.** 새 마이그레이션이 생기면 `lol-repository` 가 먼저 적용돼야 `lol-server` 가
뜬다.

의도한 제약이다. 스키마가 준비되지 않은 채 떠서 런타임에 깨지는 것보다 기동에서 멈추는
쪽이 낫다. 다만 배포 파이프라인이 이 순서를 지켜야 한다는 점은 분명히 비용이다.

느슨하게 하고 싶으면 `spring.flyway.ignore-migration-patterns` 에 `*:pending` 을 더하면
된다(체크섬·missing 검사는 유지되고 pending 만 통과). 지금은 넣지 않았다 — 순서를
지키게 만드는 쪽을 택했다.

## 환경 간 동작 차이

local 은 `migrate()`, 운영은 `validate()` — 환경마다 동작이 다른 유일한 지점이다.
일반적으로 피해야 할 모양이지만, 로컬 DB 는 개발자 1인용이고 운영 DB 는 공유 자산이라
"쓰기 주체를 하나로" 라는 요구가 로컬에는 해당하지 않는다. 차이를 남기는 대신 그 이유를
양쪽 yml 주석과 이 ADR 에 적어 둔다.

## 전제

**두 리포의 서브모듈 포인터가 같은 계보(`main`)를 가리킨다.** 이게 깨지면 `lol-server`
기동이 검증에서 멈춘다 — 같은 번호 다른 내용이면 체크섬 불일치, 한쪽이 `V38` 을 적용했는데
내 포인터에 `V37` 이 없으면 missing 으로 걸린다.

기술적 결함이 아니라 운영 규칙으로 막는다. 마이그레이션은 `lol-db-schema` 의 `main` 에만
올리고, 두 리포 모두 그 커밋을 가리킨다. `lol-db-schema/README.md` 의 번호 가드와 같은
규칙이다.

참고로 적용 주체가 하나로 유지되므로, 공동 적용안이 안고 있던 "동시 기동 시 둘이 같은
DB 에 쓴다"는 문제는 애초에 생기지 않는다.

## 버린 대안

- **운영에서도 적용(공동 소유)** — 최초 결정이었다. 같은 서브모듈 커밋이면 체크섬이 같고
  Flyway 가 히스토리 테이블에 락을 잡아 기술적으로는 안전하다. 그래도 DB 에 쓰는 경로가
  둘이 되는 것 자체가 사고 표면이고(포인터가 갈라지는 순간 운영 DB 가 걸린다), 얻는 것은
  "배포 순서를 신경 쓰지 않아도 됨" 뿐이라 접었다.
- **Flyway 를 끄고 `ddl-auto: validate` 만** — 변경 전 상태. 마이그레이션 이력 불일치를
  못 잡고, 로컬 불편도 그대로다.
- **Flyway Gradle 플러그인(`flywayValidate`)** — 접속 정보를 `build.gradle` 에 한 벌 더
  적어야 한다(플러그인은 `application.yml` 을 읽지 않는다). 다만 CI 에서 번호 충돌을
  머지 전에 잡는 용도로는 여전히 검토할 만하다.

## 영향

- `docker/Dockerfile` 이 `lol-db-schema` 를 복사한다. 빠지면 검증할 파일이 없는 이미지가 된다.
- **CI 는 서브모듈을 별도 스텝에서 받는다.** `lol-db-schema` 가 private 이라 기본
  `GITHUB_TOKEN`(워크플로가 도는 리포에만 권한) 으로는 못 받는다. 그렇다고
  `actions/checkout` 의 `token` 을 `SUBMODULE_PAT` 로 바꾸면 **메인 리포 체크아웃까지**
  그 PAT 를 타서, PAT 에 `lol-server` 권한이 없거나 만료되면 체크아웃이
  `could not read Username` 으로 죽는다(실제로 겪었다). 그래서 메인 체크아웃은 기본
  토큰으로 두고, `url.<...>.insteadOf` 로 서브모듈만 PAT 로 받는다. PAT 에는
  `lol-db-schema` 읽기 권한만 있으면 된다.
- 서브모듈이 필요한 잡에만 켠다. `deploy-service.yml` 의 `trigger-cd` 는 `build.gradle` 의
  버전만 읽으므로 켜지 않는다 — 켜면 클론 시간과 실패 지점만 늘어난다.
- 서브모듈이 비면 `processResources` 가드가 빌드를 세운다 — 조용히 빈 jar 가 나가는 것보다 낫다.
