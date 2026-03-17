# Commit, Push & PR 플랜

## Context
`feature/summoner-renewal-cache-fix` 브랜치의 모든 변경사항을 커밋하고, origin에 푸시한 뒤 PR을 생성합니다.

## 변경사항 요약
이 브랜치에는 매치 도메인 전반의 대규모 리팩토링이 포함되어 있습니다:

1. **엔티티 테이블 리네이밍**: `match_summoner` → `match_participant`, `challenges` → `match_participant_challenges`
2. **룬 구조 변경**: `StyleValue`/`StatValue` → `PerkStyleValue`/`PerkStatValue` (배열 기반 → 개별 필드)
3. **팀 정보 간소화**: `TeamInfoData`에서 championId/pickTurn 리스트 제거, `MatchBanEntity` 신규 추가
4. **엔티티 필드 추가**: patchVersion, damageDealtToEpicMonsters, atakhan/horde 관련 필드, 다수의 핑 필드
5. **타임라인 이벤트 정리**: 구 이벤트 엔티티 삭제 (EventVictimDamageDealt, EventVictimDamageReceived, TimeLineEventEntity)
6. **RestDocs 문서 정리**: 게임 아이디 리스트/게임 상세 섹션 제거
7. **ObjectMapper 설정**: `WRITE_DATES_AS_TIMESTAMPS` 비활성화 (LocalDate → yyyy-MM-dd 문자열 직렬화)
8. **QueryDSL DTO 변경**: `@QueryProjection` 제거(DailyGameCountDTO), MatchTeamDTO/SkillEventDTO 간소화

## 실행 순서

### 1. 스테이징
- 변경/삭제/신규 파일 전체를 `git add`
- `lol-db-schema` 서브모듈과 `plans/` 디렉토리는 **제외**

### 2. 커밋
```
refactor: 매치 도메인 엔티티 구조 개선 및 RestDocs 정리
```

### 3. 푸시
```bash
git push -u origin feature/summoner-renewal-cache-fix
```

### 4. PR 생성
- **base**: `main`
- **title**: `refactor: 매치 도메인 엔티티 구조 개선 및 RestDocs 정리`
- **body**: 변경사항 요약 포함
