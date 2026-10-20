#!/usr/bin/env bash
# MP-128 / MP-136 로컬 리허설 (lol-server 몫만): 빈 Postgres 에 공유 체인 V1~V35 → 01 계정 → 검증 → app 체인 거부 확인
#   → 잠금 경합 → 02 이동(Riot 테이블이 잠겨 있어도) → lol-repository 쪽 무변화 확인 → lol-server app 체인 → 검증
#   → 롤백 → 검증 → 재이동 → app 체인 → 검증
#
# 사용: docs/db-split/rehearse.sh            (lol-server 루트에서, Docker 필요)
#       KEEP=1 docs/db-split/rehearse.sh     (끝난 뒤 컨테이너를 남겨 앱 기동 리허설에 사용 — 런북 부록 참고)
#       SHARED_TARGET=36 docs/db-split/rehearse.sh  (공유 체인을 V36 까지 — lol-repository 가 V36 을 먼저 적용한 경우)
# 컨테이너 mp128-pg 가 localhost:55432 로 뜬다. 비밀번호는 실행마다 새로 만들고, KEEP=1 이면 끝에 출력한다.
# 고정하려면 POSTGRES_PASSWORD·LOL_SERVER_DB_PASSWORD 를 미리 export 한다.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
# V1~V35 는 lol-repository 배포본과 같아야 한다 — 다르면 lol-repository 기동 리허설이 체크섬 검증에서 막힌다 (런북 부록)
MIGRATIONS="$(cd "$HERE/../../lol-db-schema/db/migration" && pwd)"
APP_RESOURCES="$(cd "$HERE/../../module/app/application/src/main/resources/db" && pwd)"
SHARED_TARGET="${SHARED_TARGET:-35}"  # prod 는 V35 (lol-repository 마지막 배포 기준)
PG=mp128-pg
PORT=55432

# 리허설 전용 비밀번호 — 값을 명령행에 쓰지 않고 이름만 넘긴다(docker -e NAME)
gen_pw() { openssl rand -hex 12; }
: "${POSTGRES_PASSWORD:=$(gen_pw)}"
: "${LOL_SERVER_DB_PASSWORD:=$(gen_pw)}"
FLYWAY_PASSWORD="$POSTGRES_PASSWORD"
export POSTGRES_PASSWORD LOL_SERVER_DB_PASSWORD FLYWAY_PASSWORD

# 실패해도 컨테이너를 남기지 않는다 (KEEP=1 이면 남김)
trap '[ "${KEEP:-0}" = 1 ] || docker rm -f "$PG" >/dev/null 2>&1 || true' EXIT

psql_su() { docker exec -i "$PG" psql -U postgres -d postgres -v ON_ERROR_STOP=1 "$@"; }
roles() {
  docker exec -i -e LOL_SERVER_DB_PASSWORD \
    "$PG" psql -U postgres -d postgres -v ON_ERROR_STOP=1 -q -f - < "$HERE/01-roles.sql" > /dev/null
}
verify() { psql_su -q -f - < "$HERE/03-verify.sql" 2>&1 | grep -E 'phase|검증'; }
# lol-repository 가 기동하며 하는 일 — 지금 prod 처럼 슈퍼유저로 공유 체인을 public 에 적용
shared_flyway() {
  docker run --rm --add-host=host.docker.internal:host-gateway -e FLYWAY_PASSWORD \
    -v "$MIGRATIONS:/flyway/sql:ro" flyway/flyway:10-alpine \
    -url="jdbc:postgresql://host.docker.internal:$PORT/postgres" -user=postgres \
    -target="$SHARED_TARGET" -connectRetries=10 migrate 2>&1
}
# lol-server 가 기동하며 하는 일 — postgresql-prod.yml 의 spring.flyway 와 같은 옵션, lol_server 계정
app_flyway() {
  FLYWAY_PASSWORD="$LOL_SERVER_DB_PASSWORD" docker run --rm --add-host=host.docker.internal:host-gateway \
    -e FLYWAY_PASSWORD -v "$APP_RESOURCES:/flyway/db:ro" flyway/flyway:10-alpine \
    -url="jdbc:postgresql://host.docker.internal:$PORT/postgres" -user=lol_server \
    -locations=filesystem:/flyway/db/migration/app,filesystem:/flyway/db/migration-guard \
    -schemas=app -defaultSchema=app -createSchemas=false -baselineOnMigrate=true -baselineVersion=1 \
    -connectRetries=10 migrate 2>&1
}

docker rm -f "$PG" >/dev/null 2>&1 || true
docker run -d --name "$PG" -p "$PORT:5432" -e POSTGRES_PASSWORD postgres:16-alpine >/dev/null
until docker exec "$PG" pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done

echo "== 공유 체인 Flyway V1~V$SHARED_TARGET (슈퍼유저 — 현재 prod 와 같은 상태)"
# host.docker.internal: Docker Desktop 은 기본 제공, Linux 는 --add-host 로 지정
shared_flyway | tail -1

echo '== 01 비밀번호 누락 시 실패해야 함'
if docker exec -i "$PG" psql -U postgres -d postgres -q -f - < "$HERE/01-roles.sql" > /dev/null 2>&1; then
  echo '실패: 비밀번호 없이 종료 코드 0'; exit 1
fi
echo 'ok (종료 코드 ≠ 0)'

echo '== 01 계정·권한 (두 번 — 멱등 확인)'
roles
roles
verify

echo '== lol-server app 체인 — 이동 전에는 실패하고 아무것도 만들지 않아야 함'
if app_flyway > /dev/null; then
  echo '실패: 이동 전 app 체인이 성공함'; exit 1
fi
[ "$(psql_su -Atc "SELECT count(*) FROM pg_namespace WHERE nspname = 'app'")" = 0 ] || { echo '실패: app 스키마가 생김'; exit 1; }
echo 'ok'

echo '== 02 이동 — 다른 세션이 app 테이블 잠금을 쥐고 있으면 5초 안에 실패하고 아무것도 바뀌지 않아야 함'
psql_su -q -c 'BEGIN; LOCK TABLE public.member IN ACCESS SHARE MODE; SELECT pg_sleep(15); COMMIT;' > /dev/null &
HOLDER=$!
sleep 1
START=$(date +%s)
if psql_su -f - < "$HERE/02-move-schema.sql" > /dev/null 2>&1; then
  echo '실패: 잠금 경합 중 이동이 성공함'; exit 1
fi
echo "ok ($(( $(date +%s) - START ))초 만에 실패)"
verify
wait "$HOLDER"

echo '== 02 이동 — Riot 테이블(match)을 다른 세션이 배타 잠금으로 쥐고 있어도 막히지 않아야 함'
psql_su -q -c 'BEGIN; LOCK TABLE public.match IN ACCESS EXCLUSIVE MODE; SELECT pg_sleep(10); COMMIT;' > /dev/null &
HOLDER=$!
sleep 1
psql_su -f - < "$HERE/02-move-schema.sql" 2>&1 | grep -E 'moved tables|^ (app|public) '
verify
wait "$HOLDER"

echo '== lol-repository 쪽 무변화 — 공유 체인을 다시 돌려도 public 이 그대로여야 함 (재기동과 같은 검증)'
shared_flyway | grep -E 'up to date' || { echo '실패: 이동 후 공유 체인이 up to date 가 아님'; exit 1; }

echo '== lol-server app 체인 (V1 baseline → V2 이후 적용)'
app_flyway | grep -E 'baselined|Successfully applied|up to date'
verify

echo '== 02 롤백'
psql_su -f - < "$HERE/02-rollback.sql" 2>&1 | grep -E '^ [a-z_]+ +\| +[0-9]+$'
verify

echo '== 02 재이동'
psql_su -f - < "$HERE/02-move-schema.sql" > /dev/null 2>&1
app_flyway | grep -E 'baselined|Successfully applied|up to date'
verify

if [ "${KEEP:-0}" = 1 ]; then
  echo "== 컨테이너 $PG 유지 — 앱 기동 리허설용 비밀번호"
  echo "postgres=$POSTGRES_PASSWORD"
  echo "LOL_SERVER_DB_PASSWORD=$LOL_SERVER_DB_PASSWORD"
fi
