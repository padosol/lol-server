#!/bin/bash

# =============================================================================
# Champion Rotation API 성능 테스트 실행 스크립트
#
# 목적: Champion Rotation API 전용 성능 테스트
# 시나리오:
#   1. cache_hit_load: 캐시 히트 부하 테스트 (100 VUs, 2분)
#   2. multi_region: 멀티 리전 동시 호출 (50 VUs, 3분)
#   3. spike: 스파이크 테스트 (10 → 200 → 10 VUs)
# =============================================================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 스크립트 디렉토리 기준 경로 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K6_DIR="$SCRIPT_DIR/../k6"
SCENARIO_FILE="$K6_DIR/scenarios/champion-rotation-test.js"

# 환경 변수 기본값
BASE_URL="${BASE_URL:-http://localhost:8100}"

echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}  Champion Rotation API Performance Test${NC}"
echo -e "${BLUE}==================================================${NC}"
echo ""
echo "Target URL: $BASE_URL"
echo "Test Scenarios:"
echo "  1. Cache Hit Load (100 VUs, 2m)"
echo "  2. Multi-Region (50 VUs, 3m)"
echo "  3. Spike Test (10 → 200 → 10 VUs)"
echo ""

# k6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}Error: k6 is not installed${NC}"
    echo "Install k6: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# 서버 연결 확인
echo "Checking server connectivity..."
REGIONS=("kr" "na1" "euw1" "eun1" "jp1")
for region in "${REGIONS[@]}"; do
    if curl -s --max-time 5 "$BASE_URL/api/v1/$region/champion/rotation" > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} $region"
    else
        echo -e "  ${YELLOW}✗${NC} $region (may be slow or unavailable)"
    fi
done
echo ""

# 서버 연결 실패 시 경고
if ! curl -s --max-time 5 "$BASE_URL/api/v1/kr/champion/rotation" > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: Server may not be reachable at $BASE_URL${NC}"
    echo "Make sure the application is running."
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo -e "${GREEN}Starting Champion Rotation Performance Test...${NC}"
echo ""

# k6 실행
k6 run \
    --env BASE_URL="$BASE_URL" \
    --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
    "$SCENARIO_FILE"

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}=================================================${NC}"
    echo -e "${GREEN}  Champion Rotation Test completed successfully${NC}"
    echo -e "${GREEN}=================================================${NC}"
else
    echo -e "${RED}=================================================${NC}"
    echo -e "${RED}  Champion Rotation Test failed (exit code: $EXIT_CODE)${NC}"
    echo -e "${RED}=================================================${NC}"
fi

exit $EXIT_CODE
