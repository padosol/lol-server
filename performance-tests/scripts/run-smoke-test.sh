#!/bin/bash

# =============================================================================
# Smoke Test 실행 스크립트
#
# 목적: 기본 기능 동작 확인
# 설정: 5 VUs, 1분
# =============================================================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 스크립트 디렉토리 기준 경로 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K6_DIR="$SCRIPT_DIR/../k6"
SCENARIO_FILE="$K6_DIR/scenarios/smoke-test.js"

# 환경 변수 기본값
BASE_URL="${BASE_URL:-http://localhost:8100}"
DURATION="${DURATION:-1m}"
VUS="${VUS:-5}"

echo -e "${YELLOW}=== LOL Server Smoke Test ===${NC}"
echo "Target URL: $BASE_URL"
echo "Duration: $DURATION"
echo "VUs: $VUS"
echo ""

# k6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}Error: k6 is not installed${NC}"
    echo "Install k6: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# 서버 연결 확인
echo "Checking server connectivity..."
if ! curl -s --max-time 5 "$BASE_URL/api/v1/kr/champion/rotation" > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: Server may not be reachable at $BASE_URL${NC}"
    echo "Make sure the application is running."
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo -e "${GREEN}Starting Smoke Test...${NC}"
echo ""

# k6 실행
k6 run \
    --env BASE_URL="$BASE_URL" \
    --duration "$DURATION" \
    --vus "$VUS" \
    --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
    "$SCENARIO_FILE"

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ Smoke Test completed successfully${NC}"
else
    echo -e "${RED}✗ Smoke Test failed (exit code: $EXIT_CODE)${NC}"
fi

exit $EXIT_CODE
