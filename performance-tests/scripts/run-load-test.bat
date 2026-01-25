@echo off
REM =============================================================================
REM Load Test 실행 스크립트 (Windows)
REM
REM 목적: 핵심 사용자 여정 부하 테스트
REM 설정: 10→50 VUs 램프업, 5분
REM =============================================================================

setlocal enabledelayedexpansion

REM 환경 변수 기본값
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8100

REM 스크립트 경로 설정
set SCRIPT_DIR=%~dp0
set K6_DIR=%SCRIPT_DIR%..\k6
set SCENARIO_FILE=%K6_DIR%\scenarios\summoner-search-flow.js
set RESULTS_DIR=%SCRIPT_DIR%..\results

REM 결과 저장 디렉토리 생성
if not exist "%RESULTS_DIR%" mkdir "%RESULTS_DIR%"

REM 타임스탬프 생성
for /f "tokens=2 delims==" %%i in ('wmic os get localdatetime /value') do set datetime=%%i
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%
set RESULT_FILE=%RESULTS_DIR%\load-test-%TIMESTAMP%.json

echo ============================================
echo   LOL Server Load Test (Summoner Search)
echo ============================================
echo.
echo Configuration:
echo   Target URL: %BASE_URL%
echo   Scenario: Summoner Search Flow
echo   Stages: 10-^>30-^>50-^>30-^>0 VUs (5 min total)
echo   Result File: %RESULT_FILE%
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

echo Starting Load Test...
echo.

REM k6 실행
k6 run ^
    --env BASE_URL=%BASE_URL% ^
    --out json="%RESULT_FILE%" ^
    --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" ^
    "%SCENARIO_FILE%"

set EXIT_CODE=%errorlevel%

echo.
echo ============================================
if %EXIT_CODE% equ 0 (
    echo [SUCCESS] Load Test completed successfully
    echo.
    echo Results saved to: %RESULT_FILE%
) else (
    echo [FAILED] Load Test failed (exit code: %EXIT_CODE%)
    echo.
    echo Common failure reasons:
    echo   - Thresholds not met (p95 ^> 500ms, error rate ^> 1%)
    echo   - Server errors or timeouts
    echo   - Database connection issues
)
echo ============================================

exit /b %EXIT_CODE%
