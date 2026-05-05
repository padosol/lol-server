# 문서 유지보수 절차

CLAUDE.md, ARCHITECTURE.md, `docs/*.md` 같은 AI/팀 공유 문서가 코드와 동기화되도록 유지하는 규칙. AI agent (Claude Code) 가 잘못된 가정을 내리지 않게 하는 게 목적.

## 1. PR 시점 — 영향받는 문서를 함께 수정

코드 변경이 다음 중 하나에 해당하면 **같은 PR** 안에서 관련 CLAUDE.md/문서를 수정한다. 별도 PR 로 미루지 않는다.

| 변경 종류 | 같이 수정해야 하는 문서 |
|---|---|
| 새 모듈 추가 / 모듈 이동 / 모듈 제거 | 루트 `CLAUDE.md` 모듈 테이블, `docs/ARCHITECTURE.md` 의존 그래프 |
| 모듈 내부 구조 (Layout/Key Files) 변화 | 해당 모듈 `CLAUDE.md` Layout / Key Files |
| out port / in port 신설·시그니처 변경 | 도메인측 + 어댑터측 양쪽 모듈 CLAUDE.md (Cross-Module Dependencies) |
| 새 빌드/테스트 명령 추가 (gradle task, asciidoctor 등) | 해당 모듈 `Quick Commands` |
| 자주 밟는 함정/회귀가 새로 드러남 | 해당 모듈 `Failure Patterns / Gotchas` (❌/✅ 패턴) |
| 신규 외부 의존성 (broker, DB, OAuth provider 등) | 해당 모듈 `Boundaries` 허용/금지 목록 + `application.yml` 카탈로그 |

원칙: 코드 PR 의 리뷰어는 위 표 기준으로 **doc 누락도 review comment** 로 남길 책임이 있다.

## 2. 분기별 정기 리뷰 (3·6·9·12 월 마지막 영업일)

담당: 모듈 owner (없으면 가장 최근 커밋자).

체크리스트:

1. `gh pr list --state merged --search "merged:>$(date -d '3 months ago' +%Y-%m-%d)"` 로 머지된 PR 훑고 doc 미반영분 캡처
2. 각 모듈 CLAUDE.md 의 Key Files / Layout 이 실제 디렉토리 구조와 일치하는지 `tree -L 3` 비교
3. Failure Patterns 의 ❌/✅ 가 여전히 유효한지 (이미 수정된 함정은 제거, 새 함정은 추가)
4. broken refs 확인: `python3 ~/.claude-marketplaces/local/plugins/ai-ready-audit/skills/audit-codebase/scripts/audit.py . --pretty | jq '.signals.broken_path_refs_in_docs'`
5. 의심 모듈 1개 골라서 `/ai-ready-audit:audit-codebase` 재실행 — 점수 회귀 여부 확인

분기 리뷰 결과는 `chore/MP-<번호>-quarterly-doc-review` 브랜치 + PR 한 건으로 묶는다.

## 3. 자동 게이트 (CI)

`.github/workflows/docs-check.yml` 가 PR 단위로 다음을 검증한다:

| Job | 검증 | 실패 시 |
|---|---|---|
| `link-check` | lychee 로 `**/*.md` 파일 링크 — 깨진 file/URL refs | PR 차단 (`fail: true`) |
| `context-doc-lint` | 모든 CLAUDE.md 가 80줄 이하 ("compass, not encyclopedia") | PR 차단 |
| `context-doc-freshness` | CLAUDE.md 가 180일 넘게 미수정이면 `::warning` | 경고만 (차단 안 함) |

freshness 경고가 PR 에 떴다면 분기 리뷰가 밀린 것 — section 2 절차 트리거.

## 4. 신규 모듈 체크리스트

새 Gradle 모듈 (`module/<layer>/<name>`) 추가 시:

- [ ] `module/<layer>/<name>/CLAUDE.md` 작성 (기존 모듈 CLAUDE.md 구조 그대로 복사: Boundaries / Layout / Key Files / Common Modifications / Failure Patterns / Cross-Module Dependencies / Quick Commands / See Also)
- [ ] 80줄 이내, 5질문 (owned/modification/failure/deps/tribal) 모두 답
- [ ] 루트 `CLAUDE.md` 모듈 테이블에 한 줄 + `What to read first` 시나리오 갱신
- [ ] `docs/ARCHITECTURE.md` 의존 그래프 mermaid 노드 추가 + 영향 범위 표 갱신
- [ ] `settings.gradle` include + `app/application/build.gradle` implementation 추가

## See Also

- [`docs/workflow.md`](workflow.md) — Linear/Git 워크플로우 (브랜치/커밋/PR 규칙)
- [`docs/ARCHITECTURE.md`](ARCHITECTURE.md) — 모듈 의존 그래프 + 데이터 플로우
- [`.github/workflows/docs-check.yml`](../.github/workflows/docs-check.yml) — 자동 게이트 정의
- 루트 [`CLAUDE.md`](../CLAUDE.md) — 진입점, 모듈 카탈로그
