#!/usr/bin/env bash
# MP-136 로컬 리허설 (lol-server app 스키마만): 빈 Postgres 에 이동 전 prod 상태(public 에 app 테이블 + app 밖 테이블 하나)
#   → 01 계정 → 검증 → app 체인 거부 확인 → 잠금 경합 → 02 이동(app 밖 테이블이 잠겨 있어도)
#   → 새 이미지 롤아웃(지금 계정 + SET ROLE lol_server) → 검증 → lol_server 계정 전환 → 검증
#   → 롤백 → 검증 → 재이동 → 롤아웃 → 검증
#
# 사용: docs/db-split/rehearse.sh            (lol-server 루트에서, Docker 필요)
#       KEEP=1 docs/db-split/rehearse.sh     (끝난 뒤 컨테이너를 남겨 앱 기동 리허설에 사용 — 런북 부록 참고)
# 컨테이너 mp136-pg 가 localhost:55432 로 뜬다. 비밀번호는 실행마다 새로 만들고, KEEP=1 이면 끝에 출력한다.
# 고정하려면 POSTGRES_PASSWORD·LOL_SERVER_DB_PASSWORD 를 미리 export 한다.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
APP_RESOURCES="$(cd "$HERE/../../module/app/application/src/main/resources/db" && pwd)"
PG=mp136-pg
PORT=55432

# 리허설 전용 비밀번호 — 값을 명령행에 쓰지 않고 이름만 넘긴다(docker -e NAME)
gen_pw() { openssl rand -hex 12; }
: "${POSTGRES_PASSWORD:=$(gen_pw)}"
: "${LOL_SERVER_DB_PASSWORD:=$(gen_pw)}"
export POSTGRES_PASSWORD LOL_SERVER_DB_PASSWORD

# 실패해도 컨테이너를 남기지 않는다 (KEEP=1 이면 남김)
trap '[ "${KEEP:-0}" = 1 ] || docker rm -f "$PG" >/dev/null 2>&1 || true' EXIT

psql_su() { docker exec -i "$PG" psql -U postgres -d postgres -v ON_ERROR_STOP=1 "$@"; }
psql_app() { docker exec -i "$PG" psql -U lol_server -d postgres -v ON_ERROR_STOP=1 "$@"; }
roles() {
  docker exec -i -e LOL_SERVER_DB_PASSWORD \
    "$PG" psql -U postgres -d postgres -v ON_ERROR_STOP=1 -q -f - < "$HERE/01-roles.sql" > /dev/null
}
verify() { psql_su -q -f - < "$HERE/03-verify.sql" 2>&1 | grep -E 'phase|검증'; }
# lol-server 새 이미지가 기동하며 하는 일 — postgresql-prod.yml 의 spring.flyway 와 같은 옵션.
# $1 = 접속 계정: postgres(지금 계정 — init-sql 로 SET ROLE lol_server) / lol_server(전환 후)
app_flyway() {
  local pw="$POSTGRES_PASSWORD"
  [ "$1" = lol_server ] && pw="$LOL_SERVER_DB_PASSWORD"
  FLYWAY_PASSWORD="$pw" docker run --rm --add-host=host.docker.internal:host-gateway \
    -e FLYWAY_PASSWORD -v "$APP_RESOURCES:/flyway/db:ro" flyway/flyway:10-alpine \
    -url="jdbc:postgresql://host.docker.internal:$PORT/postgres" -user="$1" \
    -locations=filesystem:/flyway/db/migration/app,filesystem:/flyway/db/migration-guard \
    -schemas=app -defaultSchema=app -createSchemas=false -baselineOnMigrate=true -baselineVersion=1 \
    '-initSql=SET ROLE lol_server' -connectRetries=10 migrate 2>&1
}

docker rm -f "$PG" >/dev/null 2>&1 || true
docker run -d --name "$PG" -p "$PORT:5432" -e POSTGRES_PASSWORD postgres:16-alpine >/dev/null
until docker exec "$PG" pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done

echo '== 이동 전 prod 상태 — public 에 app 테이블(app 체인 V1 = 운영 V35 상태) + app 밖 테이블 하나'
{ echo 'SET search_path TO public;'
  cat "$APP_RESOURCES/migration/app/V1__init_app_schema.sql"
  echo 'CREATE TABLE public.other_service (id bigint PRIMARY KEY);'
} | psql_su -q > /dev/null
echo 'ok'

echo '== 01 비밀번호 누락 시 실패해야 함'
if docker exec -i "$PG" psql -U postgres -d postgres -q -f - < "$HERE/01-roles.sql" > /dev/null 2>&1; then
  echo '실패: 비밀번호 없이 종료 코드 0'; exit 1
fi
echo 'ok (종료 코드 ≠ 0)'

echo '== 01 계정 (두 번 — 멱등 확인)'
roles
roles
verify

echo '== lol-server 새 이미지 — 이동 전에는 실패하고 아무것도 만들지 않아야 함'
if app_flyway postgres > /dev/null; then
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

echo '== 02 이동 — app 밖 테이블을 다른 세션이 배타 잠금으로 쥐고 있어도 막히지 않아야 함'
psql_su -q -c 'BEGIN; LOCK TABLE public.other_service IN ACCESS EXCLUSIVE MODE; SELECT pg_sleep(10); COMMIT;' > /dev/null &
HOLDER=$!
sleep 1
psql_su -f - < "$HERE/02-move-schema.sql" 2>&1 | grep -E 'moved tables|^ [a-z_]+ +\|'
verify
wait "$HOLDER"

echo '== 새 이미지 롤아웃 (지금 계정 + SET ROLE lol_server — V1 baseline → V2, 새 객체도 lol_server 소유)'
app_flyway postgres | grep -E 'baselined|Successfully applied|up to date'
verify

echo '== lol_server 계정 전환 — app 체인은 그대로, app 은 읽고 app 밖은 거부돼야 함'
app_flyway lol_server | grep -E 'up to date' || { echo '실패: 전환 후 app 체인이 up to date 가 아님'; exit 1; }
psql_app -Atc 'SELECT count(*) FROM community_category' | grep -qx 8 || { echo '실패: lol_server 로 app 조회 불가'; exit 1; }
if psql_app -Atc 'SELECT count(*) FROM public.other_service' > /dev/null 2>&1; then
  echo '실패: lol_server 가 app 밖 테이블을 읽음'; exit 1
fi
echo 'ok'

echo '== 02 롤백'
psql_su -f - < "$HERE/02-rollback.sql" 2>&1 | grep -E '^ [a-z_]+ +\| +[0-9]+$'
verify

echo '== 02 재이동 → 롤아웃'
psql_su -f - < "$HERE/02-move-schema.sql" > /dev/null 2>&1
app_flyway postgres | grep -E 'baselined|Successfully applied|up to date'
verify

if [ "${KEEP:-0}" = 1 ]; then
  echo "== 컨테이너 $PG 유지 — 앱 기동 리허설용 비밀번호"
  echo "postgres=$POSTGRES_PASSWORD"
  echo "LOL_SERVER_DB_PASSWORD=$LOL_SERVER_DB_PASSWORD"
fi
