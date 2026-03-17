# Commit, Push & PR 계획

## Context
`refactor/ecr-deploy-pipeline` 브랜치에 TierFilter 도입 + 티어 계산기 리팩토링 + 리뷰 피드백 반영이 완료되었습니다. 모든 변경사항을 커밋하고 PR을 생성합니다.

## 변경사항 요약
- **`TierFilter` 값 객체 도입**: `String tier` → `TierFilter tierFilter`로 전 계층 전환
- **`ChampionTierCalculator` 알고리즘 단순화**: 시그모이드/지수 함수 기반 절대 점수 체계
- **리뷰 피드백 반영**: equals/hashCode, 공백 처리, 방어적 주석, RestDocs 설명, 테스트 구체화
- **기타**: 성능 테스트 데이터 갱신, QueryDSL 정리

## 실행 단계
1. 현재 브랜치(`refactor/ecr-deploy-pipeline`) 유지 (main이 아님)
2. 모든 변경 파일 + 신규 파일 스테이징
3. 커밋 메시지: `feat: TierFilter 값 객체 도입 및 티어 계산기 리팩토링`
4. `origin`에 푸시
5. `gh pr create`로 PR 생성 (base: main)

## 검증
- `./gradlew test` 전체 통과 확인 완료 (BUILD SUCCESSFUL)
