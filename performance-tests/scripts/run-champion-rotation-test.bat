@echo off
REM =============================================================================
REM Champion Rotation API 성능 테스트 실행 스크립트 (Windows)
REM
REM 목적: Champion Rotation API 전용 성능 테스트
REM 시나리오:
REM   1. cache_hit_load: 캐시 히트 부하 테스트 (100 VUs, 2분)
REM   2. multi_region: 멀티 리전 동시 호출 (50 VUs, 3분)
REM   3. spike: 스파이크 테스트 (10 -> 200 -> 10 VUs)
REM =============================================================================

setlocal enabledelayedexpansion

REM 환경 변수 기본값
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8100

REM 스크립트 경로 설정
set SCRIPT_DIR=%~dp0
set K6_DIR=%SCRIPT_DIR%..\k6
set SCENARIO_FILE=%K6_DIR%\scenarios\champion-rotation-test.js

echo ==================================================
echo   Champion Rotation API Performance Test
echo ==================================================
echo.
echo Target URL: %BASE_URL%
echo Test Scenarios:
echo   1. Cache Hit Load (100 VUs, 2m)
echo   2. Multi-Region (50 VUs, 3m)
echo   3. Spike Test (10 -^> 200 -^> 10 VUs)
echo.

REM k6 설치 확인
where k6 >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: k6 is not installed
    echo Install k6: https://k6.io/docs/getting-started/installation/
    echo.
    echo For Windows, use: choco install k6
    echo Or download from: https://github.com/grafana/k6/releases
    exit /b 1
)

REM 서버 연결 확인
echo Checking server connectivity...
curl -s --max-time 5 "%BASE_URL%/api/v1/kr/champion/rotation" >nul 2>nul
if %errorlevel% equ 0 (
    echo   [OK] Server is reachable
) else (
    echo   [WARNING] Server may not be reachable
    echo.
    echo Make sure the application is running at %BASE_URL%
    set /p CONTINUE="Continue anyway? (y/N): "
    if /i not "!CONTINUE!"=="y" exit /b 1
)
echo.

echo Starting Champion Rotation Performance Test...
echo.

REM k6 실행
k6 run ^
    --env BASE_URL=%BASE_URL% ^
    --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" ^
    "%SCENARIO_FILE%"

set EXIT_CODE=%errorlevel%

echo.
if %EXIT_CODE% equ 0 (
    echo ==================================================
    echo   [SUCCESS] Champion Rotation Test completed
    echo ==================================================
) else (
    echo ==================================================
    echo   [FAILED] Champion Rotation Test failed (exit code: %EXIT_CODE%)
    echo ==================================================
)

exit /b %EXIT_CODE%
