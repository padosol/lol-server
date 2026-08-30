# ADR 0003: 스키마 마이그레이션을 lol-repository 단독 소유에서 lol-server 공동 적용으로

## 상태
Accepted (2026-08-31)

## 컨텍스트

마이그레이션은 `lol-repository` 가 단독으로 적용해 왔다. `lol-server` 는
`spring.flyway.enabled: false` + `ddl-auto: validate` 로 검증만 했다.

그 대가가 로컬 개발에 그대로 걸렸다. 빈 DB 에서 `lol-server` 만 띄우면 스키마가 없어
`validate` 가 실패한다. `postgresql-local.yml` 주석이 "lol-repository 를 먼저 띄울 것"
이라고 적어 두고 있었다 — 문서로 우회한 구조적 불편이다.

두 리포는 **같은 `lol-db-schema` 서브모듈**을 싣는다. 즉 적용할 SQL 이 애초에 동일하다.

## 결정

`lol-server` 도 기동 시 Flyway 를 적용한다. local·prod 양쪽 모두 `enabled: true`.

- `flyway-core` + `flyway-database-postgresql` 을 `runtimeOnly` 로 추가한다. Flyway 10
  부터 DB 별 지원이 분리돼 core 만으로는 PostgreSQL 을 못 잡는다.
- `processResources` 가 `lol-db-schema/db/migration` 을 `db/migration` 으로 복사해
  Flyway 기본 위치(`classpath:db/migration`)에 실린다. 컨테이너에는 소스 트리가 없으므로
  `filesystem:` 로케이션은 쓸 수 없다.
- `ddl-auto: validate` 는 **유지**한다. Flyway 가 먼저 돌고 JPA 가 검증하는 순서라,
  스키마와 엔티티가 어긋나면 여전히 기동에서 잡힌다.

## 왜 안전한가

- **체크섬**: 마이그레이션 파일 내용으로 계산된다. 두 리포가 같은 서브모듈 커밋을 가리키면
  같은 파일이므로 체크섬도 같다. 서로 다른 값이 나올 여지가 없다.
- **동시 기동**: Flyway 가 스키마 히스토리 테이블에 락을 잡는다. 둘이 동시에 떠도
  한쪽이 기다렸다가 "이미 적용됨"을 보고 넘어간다.

## 남는 위험과 전제

**전제: 두 리포의 서브모듈 포인터가 같은 계보(`main`)를 가리킨다.** 이게 깨지면:

- **같은 번호, 다른 내용** — 두 브랜치가 각자 `V37` 을 쓰면 먼저 적용한 쪽이 이기고
  나머지가 체크섬 불일치로 기동 실패. `lol-db-schema/README.md` 의 번호 가드가 다루는 사고이며,
  적용 주체가 둘이 되면서 **노출 빈도가 늘어난다**.
- **건너뛴 번호** — 한쪽이 `V38` 을 적용했는데 자기 포인터에 `V37` 이 없으면 이후 검증에서
  `V37` 이 missing 으로 걸린다.

둘 다 기술적 결함이 아니라 운영 규칙으로 막는다. 마이그레이션은 `lol-db-schema` 의
`main` 에만 올리고, 두 리포 모두 그 커밋을 가리킨다.

## 버린 대안

- **local 프로파일만 켜기** — 로컬 불편은 풀리지만 운영은 그대로 단일 소유. 위험이 없는 대신
  얻는 것도 로컬 편의뿐이라, 같은 SQL 을 쓰는 마당에 환경마다 다르게 동작할 이유가 없다고 봤다.
- **Flyway Gradle 플러그인(`flywayMigrate`)** — 런타임 무변경이지만 접속 정보를
  `build.gradle` 에 한 벌 더 적어야 한다(플러그인은 `application.yml` 을 읽지 않는다).
  기동 자동 적용을 원했으므로 채택하지 않았다. 다만 `flywayValidate` 를 CI 에 거는 것은
  번호 충돌을 머지 전에 잡아 주므로 후속으로 검토할 만하다.

## 영향

- `docker/Dockerfile` 이 `lol-db-schema` 를 복사한다. 빠지면 마이그레이션 0건짜리 이미지가 된다.
- `ci.yml` · `deploy-service.yml` 의 `actions/checkout` 에 `submodules: true` 가 필요하다
  (기본값이 `false`).
- 서브모듈이 비면 `processResources` 가드가 빌드를 세운다 — 조용히 빈 jar 가 나가는 것보다 낫다.
