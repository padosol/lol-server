# Simplify Review: AI-Ready Audit ROI PR (#84)

**Date:** 2026-05-05
**Target:** PR [#84](https://github.com/padosol/lol-server/pull/84) (`chore/MP-16-ai-ready-roi-actions`) — 9 files / +136 / -0. CLAUDE.md Quick Commands 6개 추가, freshness CI job 신설, `docs/docs-maintenance.md` 신규.

---

## 1. Code Reuse Review

### Findings

| # | Item | Action |
|---|------|--------|
| 1 | `context-doc-freshness` 와 `context-doc-lint` 가 같은 `git ls-files '**/CLAUDE.md' 'CLAUDE.md'` 패턴 + while-read 골격을 두 번 작성. 두 job 모두 lightweight bash. | **Skip** — 통합 시 lint 도 `fetch-depth: 0` 비용을 떠안음. job 분리는 fail-fast 격리/캐시 친화성 측면에서 가치 있어 현 구조 유지가 합리적. |
| 2 | 6개 모듈 Quick Commands 포맷이 기존 postgresql/api/domain 패턴 (` ```bash + 한국어 주석 + 3줄 이내`) 과 일치 | **Skip (이미 OK)** — 재사용 측면 이슈 없음. |
| 3 | `docs-maintenance.md` 와 `docs/workflow.md` 영역 분리: 전자는 doc 동기화 규칙, 후자는 Linear/Git 흐름. Cross-reference 만 있고 내용 중복 없음. | **Skip (이미 OK)** |

---

## 2. Code Quality Review

### Findings

| # | Item | Severity | Action |
|---|------|----------|--------|
| 1 | freshness job 의 PR trigger 가 `**/*.md` paths 필터에 묶여 코드-only PR (= stale 신호가 가장 의미 있는 케이스) 에서 아예 실행 안 됨. 의도와 정반대. | **Medium** | **Fixed** — `schedule: cron '0 9 * * 1'` (매주 월 09:00 UTC) 트리거 추가. PR 트리거는 markdown 변경 PR 한정, 주간 cron 으로 코드-only 케이스 커버. |
| 2 | bash 스크립트의 `stale_count=$stale` 가 echo 만 되고 어디에도 활용 안 되는 dead variable. step output 으로 노출하지 않으므로 노이즈. | **Low** | **Fixed** — 변수 + echo 둘 다 제거. |
| 3 | `\|\| echo ""` 방어 코드: `set -e` 하에 `git log -1 --format=%ct` 가 비-0 종료하는 케이스는 사실상 없음. 그러나 `[ -z "$ts" ]` 가드와 함께 안전. | **Low** | **Skip** — 무해, 변경 시 신규 엣지 케이스 도입 위험. |
| 4 | `set -euo pipefail`, `[ -gt ]` 정수 비교, process substitution scope (subshell 회피), 모듈명 prefix 정확성 (settings.gradle 일치), `subprojects { apply plugin: 'checkstyle' }` 로 모든 모듈 `checkstyleMain` task 존재 확인. | — | **Skip (이미 OK)** |
| 5 | markdown: 80줄 cap 통과 (CLAUDE.md 변경 6개 모두 58-69줄, docs-maintenance.md 61줄), 신규 cross-link 4개 모두 실존, `Closes MP-16` 본문 미포함. | — | **Skip (이미 OK)** |

---

## 3. Efficiency Review

### Findings

| # | Item | Impact | Action |
|---|------|--------|--------|
| 1 | freshness trigger 잘못 (위 Quality #1 와 동일 finding 의 효율 측면) — markdown PR 만 검사하면 critical-path 비용은 적지만 시그널 자체가 의미 없음. | **Medium** | **Fixed** — cron 트리거로 시그널 품질 회복. |
| 2 | `fetch-depth: 0` full clone 이 11개 파일 mtime 만 위해 과함. `filter=blob:none` 또는 lookback-bounded shallow depth 가능. | **Medium** | **Skip** — cron 으로 옮긴 후 critical path 가 아님. 리포 작은 단계에서 마이크로 최적화. 50+ CLAUDE.md 로 늘면 재검토. |
| 3 | N=11 × `git log -1` 호출 → 단일 `git log --name-only` 로 묶기 가능. | **Negligible** | **Skip** — 수백 ms 차이, 가독성 trade-off. |
| 4 | `link-check` / `lint` / `freshness` 3-job 으로 fan-out → 각자 actions/checkout 비용 (~5-10s × 3). | **Low** | **Skip** — fail isolation + 병렬 실행 이점이 더 큼. |
| 5 | Quick Commands / maintenance doc — 효율 영향 없음. | **Negligible** | **Skip** |

---

## Summary

| File | Change |
|------|--------|
| `.github/workflows/docs-check.yml` | `schedule: cron '0 9 * * 1'` 트리거 추가 (`on:` 블록), freshness job 의 dead `stale_count=$stale` echo 제거 |
| `docs/docs-maintenance.md` | CI 게이트 표 row 에 트리거 조건 명시 (PR + 주간 cron), 추가 한 줄 — markdown 변경 없는 PR 의 stale 신호는 cron run 으로만 잡힘 |

**Build result:** ✅ BUILD SUCCESSFUL — follow-up commit `efba9a4` 에서 CI 7/7 모두 pass (Build & Test 2m5s, Checkstyle 1m16s, CLAUDE.md Freshness 6s, CLAUDE.md Lint 3s, Markdown Link Check 8s, GitGuardian, Slack Notification).

**Findings 집계:** Reuse 0 fix / 3 skip · Quality 2 fix / 3 skip · Efficiency 1 fix / 4 skip · 총 **3 fix / 10 skip**.
