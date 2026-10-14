#!/usr/bin/env bash
# MP-128 로컬 리허설: 빈 Postgres 에 V1~Vn 적용 → 01 계정 → 검증 → 잠금 경합 → 02 이동 → 검증 → 롤백 → 검증 → 재이동 → 검증
#
# 사용: docs/db-split/rehearse.sh            (lol-server 루트에서, Docker 필요)
#       KEEP=1 docs/db-split/rehearse.sh     (끝난 뒤 컨테이너를 남겨 앱 기동 리허설에 사용 — 런북 부록 참고)
# 컨테이너 mp128-pg 가 localhost:55432 로 뜬다. 비밀번호는 실행마다 새로 만들고, KEEP=1 이면 끝에 출력한다.
# 고정하려면 POSTGRES_PASSWORD·LOL_SERVER_DB_PASSWORD·LOL_REPOSITORY_DB_PASSWORD 를 미리 export 한다.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
MIGRATIONS="$(cd "$HERE/../../lol-db-schema/db/migration" && pwd)"
PG=mp128-pg
PORT=55432

# 리허설 전용 비밀번호 — 값을 명령행에 쓰지 않고 이름만 넘긴다(docker -e NAME)
gen_pw() { openssl rand -hex 12; }
: "${POSTGRES_PASSWORD:=$(gen_pw)}"
: "${LOL_SERVER_DB_PASSWORD:=$(gen_pw)}"
: "${LOL_REPOSITORY_DB_PASSWORD:=$(gen_pw)}"
FLYWAY_PASSWORD="$POSTGRES_PASSWORD"
export POSTGRES_PASSWORD LOL_SERVER_DB_PASSWORD LOL_REPOSITORY_DB_PASSWORD FLYWAY_PASSWORD

# 실패해도 컨테이너를 남기지 않는다 (KEEP=1 이면 남김)
trap '[ "${KEEP:-0}" = 1 ] || docker rm -f "$PG" >/dev/null 2>&1 || true' EXIT

psql_su() { docker exec -i "$PG" psql -U postgres -d postgres -v ON_ERROR_STOP=1 "$@"; }
roles() {
  docker exec -i -e LOL_SERVER_DB_PASSWORD -e LOL_REPOSITORY_DB_PASSWORD \
    "$PG" psql -U postgres -d postgres -v ON_ERROR_STOP=1 -q -f - < "$HERE/01-roles.sql" > /dev/null
}
verify() { psql_su -q -f - < "$HERE/03-verify.sql" 2>&1 | grep -E 'phase|검증'; }

docker rm -f "$PG" >/dev/null 2>&1 || true
docker run -d --name "$PG" -p "$PORT:5432" -e POSTGRES_PASSWORD postgres:16-alpine >/dev/null
until docker exec "$PG" pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done

echo '== Flyway V1~Vn (슈퍼유저 — 현재 prod 와 같은 상태)'
# host.docker.internal: Docker Desktop 은 기본 제공, Linux 는 --add-host 로 지정
docker run --rm --add-host=host.docker.internal:host-gateway -e FLYWAY_PASSWORD \
  -v "$MIGRATIONS:/flyway/sql:ro" flyway/flyway:10-alpine \
  -url="jdbc:postgresql://host.docker.internal:$PORT/postgres" -user=postgres \
  -connectRetries=10 migrate | tail -1

echo '== 01 비밀번호 누락 시 실패해야 함'
if docker exec -i "$PG" psql -U postgres -d postgres -q -f - < "$HERE/01-roles.sql" > /dev/null 2>&1; then
  echo '실패: 비밀번호 없이 종료 코드 0'; exit 1
fi
echo 'ok (종료 코드 ≠ 0)'

echo '== 01 계정·권한 (두 번 — 멱등 확인)'
roles
roles
verify

echo '== 02 이동 — 다른 세션이 잠금을 쥐고 있으면 5초 안에 실패하고 아무것도 바뀌지 않아야 함'
psql_su -q -c 'BEGIN; LOCK TABLE public.match IN ACCESS SHARE MODE; SELECT pg_sleep(15); COMMIT;' > /dev/null &
HOLDER=$!
sleep 1
START=$(date +%s)
if psql_su -f - < "$HERE/02-move-schema.sql" > /dev/null 2>&1; then
  echo '실패: 잠금 경합 중 이동이 성공함'; exit 1
fi
echo "ok ($(( $(date +%s) - START ))초 만에 실패)"
verify
wait "$HOLDER"

echo '== 02 이동'
psql_su -f - < "$HERE/02-move-schema.sql" 2>&1 | grep -E 'moved tables|^ (app|public|riot) '
verify

echo '== 02 롤백'
psql_su -f - < "$HERE/02-rollback.sql" 2>&1 | grep -E 'lol_repository \|'
verify

echo '== 02 재이동'
psql_su -f - < "$HERE/02-move-schema.sql" > /dev/null 2>&1
verify

if [ "${KEEP:-0}" = 1 ]; then
  echo "== 컨테이너 $PG 유지 — 앱 기동 리허설용 비밀번호"
  echo "postgres=$POSTGRES_PASSWORD"
  echo "LOL_SERVER_DB_PASSWORD=$LOL_SERVER_DB_PASSWORD"
  echo "LOL_REPOSITORY_DB_PASSWORD=$LOL_REPOSITORY_DB_PASSWORD"
fi
