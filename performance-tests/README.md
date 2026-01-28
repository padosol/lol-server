# Performance Tests

LOL Server API 성능 테스트 환경입니다.

## 사전 요구사항

### k6 설치

**Windows (Chocolatey):**
```powershell
choco install k6
```

**macOS (Homebrew):**
```bash
brew install k6
```

**Linux:**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Docker:**
```bash
docker pull grafana/k6
```

## 디렉토리 구조

```
performance-tests/
├── k6/
│   ├── scenarios/
│   │   ├── smoke-test.js           # 기본 기능 확인 테스트
│   │   └── summoner-search-flow.js # 소환사 검색 부하 테스트
│   ├── lib/
│   │   ├── config.js               # 공통 설정
│   │   └── helpers.js              # 헬퍼 함수
│   └── thresholds.js               # 성능 임계값 정의
├── data/
│   └── test-summoners.json         # 테스트 데이터
├── scripts/
│   ├── run-smoke-test.sh           # Smoke Test 실행 (Linux/macOS)
│   ├── run-smoke-test.bat          # Smoke Test 실행 (Windows)
│   ├── run-load-test.sh            # Load Test 실행 (Linux/macOS)
│   └── run-load-test.bat           # Load Test 실행 (Windows)
└── results/                        # 테스트 결과 저장 디렉토리
```

## 테스트 실행

### 1. 인프라 시작

```bash
cd docker
docker-compose -f docker-compose.yaml -f docker-compose.perf.yaml up -d
```

### 2. 애플리케이션 시작

```bash
./gradlew bootRun -Dspring.profiles.active=local
```

### 3. Smoke Test 실행

기본 기능 동작 확인 (5 VUs, 1분)

**Windows:**
```cmd
cd performance-tests\scripts
run-smoke-test.bat
```

**Linux/macOS:**
```bash
cd performance-tests/scripts
chmod +x run-smoke-test.sh
./run-smoke-test.sh
```

### 4. Load Test 실행

소환사 검색 플로우 부하 테스트 (10→50 VUs, 5분)

**Windows:**
```cmd
cd performance-tests\scripts
run-load-test.bat
```

**Linux/macOS:**
```bash
cd performance-tests/scripts
chmod +x run-load-test.sh
./run-load-test.sh
```

### 직접 k6 실행

```bash
# Smoke Test
k6 run --env BASE_URL=http://localhost:8100 k6/scenarios/smoke-test.js

# Load Test
k6 run --env BASE_URL=http://localhost:8100 k6/scenarios/summoner-search-flow.js
```

## 테스트 시나리오

### Smoke Test

- **목적**: 기본 기능 동작 확인
- **설정**: 5 VUs, 1분
- **대상 API**:
  - Champion Rotation (캐시)
  - Autocomplete 검색
  - Summoner 상세 조회
  - League 정보 조회
  - Match 목록 조회
  - Rank Champions 통계

### Summoner Search Flow

- **목적**: 핵심 사용자 여정 부하 테스트
- **설정**: 10→50 VUs 램프업, 5분
- **플로우**:
  1. 자동완성 검색 (5회 - 타이핑 시뮬레이션)
  2. 소환사 상세 조회
  3. 리그 정보 조회
  4. 매치 목록 조회 (3페이지)
  5. 랭크 챔피언 통계

## 성능 목표 (임계값)

| API | p95 응답시간 | 에러율 |
|-----|-------------|--------|
| Summoner 검색 | < 300ms | < 0.1% |
| Autocomplete | < 150ms | < 0.1% |
| Match 목록 | < 500ms | < 0.5% |
| League 조회 | < 300ms | < 0.5% |
| Champion Rotation | < 100ms | < 0.01% |
| Rank Champions | < 400ms | < 0.5% |

**전체 기준:**
- p95 < 500ms
- p99 < 1000ms
- 에러율 < 1%

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `BASE_URL` | `http://localhost:8100` | 테스트 대상 서버 URL |
| `DURATION` | `1m` | Smoke Test 실행 시간 |
| `VUS` | `5` | Smoke Test 가상 사용자 수 |

## 결과 분석

### 콘솔 출력

k6 실행 후 콘솔에서 다음을 확인:

- ✓ (체크) 표시: threshold 통과
- ✗ (엑스) 표시: threshold 실패
- 종료 코드 0: 모든 threshold 통과

### JSON 결과 파일

Load Test 실행 시 `results/` 디렉토리에 JSON 파일 저장:

```bash
results/load-test-20250123_143000.json
```

## Docker에서 k6 실행

```bash
# docker-compose.perf.yaml 사용
docker-compose -f docker-compose.yaml -f docker-compose.perf.yaml run --rm k6 run /tests/k6/scenarios/smoke-test.js
```

## 문제 해결

### 서버 연결 실패

```
Warning: Server may not be reachable at http://localhost:8100
```

- 애플리케이션이 실행 중인지 확인
- 포트 충돌 여부 확인
- 방화벽 설정 확인

### Threshold 실패

```
✗ http_req_duration..............: avg=523ms
```

- 서버 로그 확인
- 데이터베이스 연결 풀 확인
- 외부 API (Riot API) 응답 시간 확인

### 높은 에러율

```
✗ http_req_failed................: 5.23%
```

- 테스트 데이터 확인 (test-summoners.json)
- API 엔드포인트 정상 동작 확인
- Rate Limiting 설정 확인
