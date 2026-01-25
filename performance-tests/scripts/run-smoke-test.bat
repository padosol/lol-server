@echo off
REM =============================================================================
REM Smoke Test 실행 스크립트 (Windows)
REM
REM 목적: 기본 기능 동작 확인
REM 설정: 5 VUs, 1분
REM =============================================================================

setlocal enabledelayedexpansion

REM 환경 변수 기본값
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8100
if "%DURATION%"=="" set DURATION=1m
if "%VUS%"=="" set VUS=5

REM 스크립트 경로 설정
set SCRIPT_DIR=%~dp0
set K6_DIR=%SCRIPT_DIR%..\k6
set SCENARIO_FILE=%K6_DIR%\scenarios\smoke-test.js

echo ===========================================
echo   LOL Server Smoke Test
echo ===========================================
echo.
echo Target URL: %BASE_URL%
echo Duration: %DURATION%
echo VUs: %VUS%
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

echo Starting Smoke Test...
echo.

REM k6 실행
k6 run ^
    --env BASE_URL=%BASE_URL% ^
    --duration %DURATION% ^
    --vus %VUS% ^
    --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" ^
    "%SCENARIO_FILE%"

set EXIT_CODE=%errorlevel%

echo.
if %EXIT_CODE% equ 0 (
    echo [SUCCESS] Smoke Test completed successfully
) else (
    echo [FAILED] Smoke Test failed (exit code: %EXIT_CODE%)
)

exit /b %EXIT_CODE%
