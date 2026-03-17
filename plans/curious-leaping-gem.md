# PostgreSQL 모듈 헥사고날 아키텍처 준수 분석 및 개선 플랜

## Context

PostgreSQL 영속성 모듈(`module/infra/persistence/postgresql`)이 프로젝트의 헥사고날 아키텍처 규칙을 올바르게 준수하고 있는지 전수 분석. 9개 도메인의 어댑터, 엔티티, 매퍼, 리포지토리, DTO 구조를 포트 인터페이스와 대조 검증함.

---

## 준수 사항 (정상)

| 항목 | 상태 |
|------|------|
| 9개 어댑터가 outbound port 인터페이스 구현 | OK |
| 다른 infra 모듈 간 교차 의존 없음 | OK |
| 도메인 로직이 영속성 계층에 누출되지 않음 | OK |
| MapStruct로 Entity↔Domain 분리 | OK |
| QueryDSL Custom Repository 패턴 | OK |
| `*Entity`, `*DTO`, `*PersistenceAdapter`, `*Mapper` 명명 규칙 | OK |
| Champion 포트가 Redis/ClickHouse에서 구현됨 (아키텍처적으로 유효) | OK |

---

## 발견된 이슈 (5건)

### Issue 1: `ChampionRotatePort` 미사용 데드 코드 — HIGH

`ChampionRotatePort`는 어디에서도 구현/참조되지 않는 완전한 데드 코드. `ChampionPersistencePort`의 이전 버전으로 추정됨 (`platformId` 파라미터 없는 동일 시그니처).

**수정:**
- 삭제: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/champion/application/port/out/ChampionRotatePort.java`

---

### Issue 2: Summoner 패키지 구조 불일치 — HIGH

Summoner만 `adapter/`, `mapper/` 서브디렉토리 없이 루트 레벨에 파일 배치. 다른 8개 도메인 모두 서브디렉토리 사용.

**현재 (잘못됨):**
```
repository/summoner/
├── SummonerPersistenceAdapter.java   ← 루트
├── SummonerMapper.java               ← 루트
```

**기대 (올바름):**
```
repository/summoner/
├── adapter/SummonerPersistenceAdapter.java
├── mapper/SummonerMapper.java
```

**수정 대상 파일:**
1. `repository/summoner/SummonerPersistenceAdapter.java` → `repository/summoner/adapter/`로 이동, 패키지 선언 변경
2. `repository/summoner/SummonerMapper.java` → `repository/summoner/mapper/`로 이동, 패키지 선언 변경
3. `SummonerPersistenceAdapter.java` 내 SummonerMapper import 경로 수정
4. 테스트 `SummonerPersistenceAdapterTest.java` — 패키지 및 import 수정
5. 테스트 `SummonerMapperTest.java` — SummonerMapper import 수정

**Spring 스캐닝 영향:** 없음 (`com.example.lolserver` 하위 전체 스캔)

---

### Issue 3: League 포트 패키지 위치 불일치 — MODERATE

`LeaguePersistencePort`가 `application.port`에 위치. 다른 모든 outbound 포트는 `application.port.out`에 위치.

**수정 대상 파일:**
1. `domain/league/application/port/LeaguePersistencePort.java` → `port/out/`으로 이동
2. `LeagueService.java` — import 수정
3. `LeagueServiceTest.java` — import 수정
4. `LeaguePersistenceAdapter.java` — import 수정

---

### Issue 4: JPA Repository 명명 불일치 — LOW (선택적)

`*Repository` (5개)와 `*JpaRepository` (5개)가 혼재. 기능 영향 없으나 일관성 부족.

- `QueueTypeRepository`, `MatchRepository` 등 → `*JpaRepository`로 통일 권장
- **별도 PR로 진행 권장**, 필수 아님

---

## 실행 순서

| 순서 | 이슈 | 작업량 | 리스크 |
|------|------|--------|--------|
| 1 | Issue 1: `ChampionRotatePort` 삭제 | 파일 1개 삭제 | 없음 |
| 2 | Issue 2: Summoner 패키지 구조 정리 | 파일 2개 이동 + import 4~5곳 수정 | 낮음 |
| 3 | Issue 3: League 포트 패키지 이동 | 파일 1개 이동 + import 3곳 수정 | 낮음 |
| 4 | Issue 4: Repository 명명 통일 (선택적) | 파일 8+개 리네임 | 매우 낮음 |

Issue 1~3은 하나의 PR에 커밋 3개로 진행. Issue 4는 별도 PR로 분리.

---

## 검증 방법

```bash
# 빌드 및 테스트 통과 확인
./gradlew clean build

# 패키지 구조 검증
find module/infra/persistence/postgresql -name "*Adapter.java" -not -path "*/adapter/*"
# → 결과 없어야 함 (모든 어댑터가 adapter/ 하위에 있어야 함)

find module/infra/persistence/postgresql -name "*Mapper.java" -not -path "*/mapper/*"
# → 결과 없어야 함

# 데드 코드 확인
grep -r "ChampionRotatePort" module/
# → 결과 없어야 함
```
