#!/usr/bin/env bash
# MP-128 로컬 리허설: 빈 Postgres 에 V1~Vn 적용 → 01 계정 → 검증 → 02 이동 → 검증 → 롤백 → 검증 → 재이동 → 검증
#
# 사용: docs/db-split/rehearse.sh            (lol-server 루트에서, Docker 필요)
#       KEEP=1 docs/db-split/rehearse.sh     (끝난 뒤 컨테이너를 남겨 앱 기동 리허설에 사용 — 런북 부록 참고)
# 컨테이너 mp128-pg 가 localhost:55432 로 뜬다. 비밀번호: postgres/1234, lol_server/srvpw, lol_repository/repopw
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
MIGRATIONS="$(cd "$HERE/../../lol-db-schema/db/migration" && pwd)"
PG=mp128-pg
PORT=55432

psql_su() { docker exec -i "$PG" psql -U postgres -d postgres -v ON_ERROR_STOP=1 "$@"; }

docker rm -f "$PG" >/dev/null 2>&1 || true
docker run -d --name "$PG" -p "$PORT:5432" -e POSTGRES_PASSWORD=1234 postgres:16-alpine >/dev/null
until docker exec "$PG" pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done

echo '== Flyway V1~Vn (슈퍼유저 — 현재 prod 와 같은 상태)'
docker run --rm -v "$MIGRATIONS:/flyway/sql:ro" flyway/flyway:10-alpine \
  -url="jdbc:postgresql://host.docker.internal:$PORT/postgres" -user=postgres -password=1234 \
  -connectRetries=10 migrate | tail -1

echo '== 01 계정·권한 (두 번 — 멱등 확인)'
for _ in 1 2; do
  psql_su -q -v lol_server_password=srvpw -v lol_repository_password=repopw -f - < "$HERE/01-roles.sql" > /dev/null
done
psql_su -q -f - < "$HERE/03-verify.sql" 2>&1 | grep -E 'phase|검증'

echo '== 02 이동'
psql_su -f - < "$HERE/02-move-schema.sql" 2>&1 | grep -E 'moved tables|^ (app|public|riot) '
psql_su -q -f - < "$HERE/03-verify.sql" 2>&1 | grep -E 'phase|검증'

echo '== 02 롤백'
psql_su -f - < "$HERE/02-rollback.sql" 2>&1 | grep -E 'lol_repository \|'
psql_su -q -f - < "$HERE/03-verify.sql" 2>&1 | grep -E 'phase|검증'

echo '== 02 재이동'
psql_su -f - < "$HERE/02-move-schema.sql" > /dev/null 2>&1
psql_su -q -f - < "$HERE/03-verify.sql" 2>&1 | grep -E 'phase|검증'

if [ "${KEEP:-0}" != 1 ]; then docker rm -f "$PG" >/dev/null; fi
