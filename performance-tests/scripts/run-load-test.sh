#!/bin/bash

# =============================================================================
# Load Test 실행 스크립트 (Summoner Search Flow)
#
# 목적: 핵심 사용자 여정 부하 테스트
# 설정: 10→50 VUs 램프업, 5분
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
SCENARIO_FILE="$K6_DIR/scenarios/summoner-search-flow.js"
RESULTS_DIR="$SCRIPT_DIR/../results"

# 환경 변수 기본값
BASE_URL="${BASE_URL:-http://localhost:8100}"

# 결과 저장 디렉토리 생성
mkdir -p "$RESULTS_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_FILE="$RESULTS_DIR/load-test-$TIMESTAMP.json"

echo -e "${YELLOW}============================================${NC}"
echo -e "${YELLOW}  LOL Server Load Test (Summoner Search)${NC}"
echo -e "${YELLOW}============================================${NC}"
echo ""
echo -e "${BLUE}Configuration:${NC}"
echo "  Target URL: $BASE_URL"
echo "  Scenario: Summoner Search Flow"
echo "  Stages: 10→30→50→30→0 VUs (5 min total)"
echo "  Result File: $RESULT_FILE"
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
    echo "Make sure the application is running with:"
    echo "  ./gradlew bootRun -Dspring.profiles.active=local"
    echo ""
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}Starting Load Test...${NC}"
echo ""

# k6 실행
k6 run \
    --env BASE_URL="$BASE_URL" \
    --out json="$RESULT_FILE" \
    --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
    "$SCENARIO_FILE"

EXIT_CODE=$?

echo ""
echo -e "${YELLOW}============================================${NC}"

if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ Load Test completed successfully${NC}"
    echo ""
    echo -e "${BLUE}Results saved to:${NC} $RESULT_FILE"
else
    echo -e "${RED}✗ Load Test failed (exit code: $EXIT_CODE)${NC}"
    echo ""
    echo "Common failure reasons:"
    echo "  - Thresholds not met (p95 > 500ms, error rate > 1%)"
    echo "  - Server errors or timeouts"
    echo "  - Database connection issues"
fi

echo -e "${YELLOW}============================================${NC}"

exit $EXIT_CODE
