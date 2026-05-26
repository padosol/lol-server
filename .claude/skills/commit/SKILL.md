---
name: commit
description: lol-server 저장소에서 git 커밋을 만들 때 사용한다. 커밋 메시지를 이 repo 컨벤션(`<type>: MP-<번호> <한글 설명>`)에 맞추고, Claude/AI 자동 서명(`Co-Authored-By: Claude`, `🤖 Generated with ...` 등)을 절대 넣지 않도록 보장한다. 사용자가 "커밋해줘", "commit", "변경사항 커밋" 등을 요청하거나 `git commit`을 실행하려 할 때 반드시 사용한다.
---

# Commit

이 저장소의 커밋 메시지 규칙. 목적은 (1) 일관된 컨벤션, (2) AI 자동 서명이 이력에 새어 들어가지 않게 하는 것.

## 메시지 형식

`<type>: MP-<번호> <한글 설명>`

- **type**: `feat` · `fix` · `refactor` · `docs` · `chore` 중 하나
- **MP-<번호>**: 관련 Linear 이슈 키. 이슈가 있으면 포함하고, 없으면 사용자에게 확인하거나 생략한다(`<type>: <한글 설명>`).
- **설명**: 한글로, 무엇을 왜 바꿨는지 간결하게.

추가 맥락이 필요하면 제목 다음 빈 줄 뒤 본문에 한글로 적는다.

## 절대 금지: AI 자동 서명

커밋 메시지(제목·본문·트레일러 어디에도)에 다음을 **넣지 않는다**:

- `Co-Authored-By: Claude ...`
- `🤖 Generated with ...` 같은 AI 생성 표기

이유: 한 번 push된 커밋 메시지는 force-push로만 바꿀 수 있고, 그래도 PR 타임라인의 force-push 기록과 dangling 커밋에 흔적이 남아 사실상 완전히 지울 수 없다. 처음부터 넣지 않는 것이 유일하게 깨끗한 방법이다.

## 예시

변경: Tomcat work 디렉토리를 `.gitignore`에 추가
```
chore: 내장 Tomcat work 디렉토리 .gitignore에 추가
```

변경: 소환사 검색 기능 추가 (Linear MP-7)
```
feat: MP-7 소환사 검색 API 추가
```
