---
name: pr
description: lol-server 저장소에서 Pull Request를 만들 때 사용한다. PR 본문을 저장소의 `.github/PULL_REQUEST_TEMPLATE.md` 구조 그대로 채우고, 베이스 브랜치는 git-flow에 따라 `develop`으로 잡으며, Claude/AI 생성 표기(`🤖 Generated with ...`, `Co-Authored-By: Claude`)를 본문에 넣지 않도록 보장한다. 사용자가 "PR 만들어줘", "PR 올려줘", "pull request 생성" 등을 요청하거나 `gh pr create`를 실행하려 할 때 반드시 사용한다.
---

# Pull Request

이 저장소에서 PR을 만들 때의 규칙.

## 1. 베이스 브랜치

git-flow를 따른다: 작업 브랜치 → `develop`. 기본은 `--base develop`. (긴급 `hotfix`만 `main`.)

## 2. 본문은 템플릿을 따른다

PR 본문은 **`.github/PULL_REQUEST_TEMPLATE.md`를 읽어** 그 섹션 구조를 그대로 채운다. 템플릿은 바뀔 수 있으니 매번 파일을 읽어 최신 섹션을 확인하고, 섹션 목록을 여기에 하드코딩하지 않는다.

채울 때 주의:
- **변경 유형**: 해당하는 체크박스를 `- [x]`로 표시한다.
- **테스트**: 실제로 실행·검증한 항목만 체크한다. 안 했거나 해당 없으면 체크하지 말고 사유(예: `N/A — 코드 변경 없음`)를 적는다.
- **관련 이슈**: Linear `MP-<번호>`가 있으면 링크하고, 없으면 사용자에게 확인하거나 `N/A`로 둔다.

## 3. 절대 금지: AI 생성 표기

PR 본문에 `🤖 Generated with Claude Code`, `Co-Authored-By: Claude` 등 AI 표기를 넣지 않는다. 한 번 만들어진 PR과 그 타임라인은 삭제할 수 없으므로 처음부터 넣지 않는다.

## 만드는 법

```bash
gh pr create --base develop \
  --title "<type>: <한글 제목>" \
  --body "<템플릿을 읽어 채운 본문>"
```
