# Husky 도입 및 Pre-commit Checkstyle Hook 설정

## Context

현재 프로젝트는 Checkstyle이 Gradle에 완벽히 구성되어 있지만(`build.gradle` L54-88), Git hooks가 없어 커밋 전 코드 스타일 위반을 로컬에서 잡지 못합니다. CI에서만 Checkstyle을 검사하므로, PR 단계에서야 위반이 발견되어 불필요한 피드백 루프가 발생합니다.

Husky를 도입하여 pre-commit hook으로 커밋 전 Checkstyle을 자동 실행하고, 위반 시 커밋을 차단합니다.

## 현재 상태

- **Checkstyle 설정 완료**: `build.gradle`에 checkstyle 플러그인 적용 (v10.21.4, maxWarnings=0, maxErrors=0)
- **설정 파일**: `config/checkstyle/checkstyle.xml`, `suppressions.xml`
- **차등 검사 지원**: `-PcheckstyleDiff -PcheckstyleBaseRef=<ref>` 프로퍼티로 변경 파일만 검사 가능 (`build.gradle` L71-88)
- **Git hooks 없음**: `.git/hooks/`에 sample 파일만 존재
- **package.json 없음**: 순수 Java/Gradle 프로젝트

## 설계 결정

**lint-staged 미사용**: Gradle checkstyle은 source set 단위로 동작하여 개별 파일 경로를 받지 않음. 프로젝트에 이미 `checkstyleDiff` 메커니즘이 있으므로 lint-staged 없이 Husky만 사용.

## 구현 단계

### 1. `package.json` 생성 (신규)

```json
{
  "private": true,
  "scripts": {
    "prepare": "husky"
  },
  "devDependencies": {
    "husky": "^9.1.0"
  }
}
```

- `"private": true` — npm 배포 방지
- `"prepare": "husky"` — `npm install` 시 Husky 자동 초기화

### 2. Husky 설치 및 초기화

```bash
npm install
npx husky init
```

### 3. `.husky/pre-commit` 작성 (신규)

```bash
#!/usr/bin/env bash

echo "=== Pre-commit: Checkstyle 검사 ==="

# 스테이징된 Java 파일 확인
STAGED_JAVA_FILES=$(git diff --cached --name-only --diff-filter=ACMR -- '*.java')

if [ -z "$STAGED_JAVA_FILES" ]; then
    echo "Java 파일 변경 없음. Checkstyle 건너뜀."
    exit 0
fi

# 생성 코드 제외
RELEVANT_FILES=$(echo "$STAGED_JAVA_FILES" | grep -v -E '(Q[A-Z].*\.java|.*MapperImpl\.java)')

if [ -z "$RELEVANT_FILES" ]; then
    echo "생성 코드만 변경됨. Checkstyle 건너뜀."
    exit 0
fi

FILE_COUNT=$(echo "$RELEVANT_FILES" | wc -l | tr -d ' ')
echo "${FILE_COUNT}개 파일 검사 중..."

# 기존 checkstyleDiff 메커니즘 활용 (HEAD 대비 변경 파일만 검사)
./gradlew checkstyleMain checkstyleTest -PcheckstyleDiff -PcheckstyleBaseRef=HEAD --daemon --quiet

if [ $? -ne 0 ]; then
    echo ""
    echo "=== Checkstyle 실패 ==="
    echo "위반 사항을 수정한 후 커밋하세요."
    echo "상세 보고서: ./gradlew checkstyleMain checkstyleTest"
    echo "긴급 우회: git commit --no-verify"
    exit 1
fi

echo "=== Checkstyle 통과 ==="
```

### 4. `.gitignore` 수정

`node_modules/` 추가 (L61 이후):

```
### Node.js (Husky) ###
node_modules/
```

### 5. `CONTRIBUTING.md` 수정 (L104 이후에 추가)

Git Hooks 섹션 추가 — Husky 설치 방법, pre-commit 동작 설명

### 6. `CLAUDE.md` 수정

Git 워크플로우 섹션에 pre-commit hook 설명 추가

## 수정 대상 파일

| 파일 | 작업 |
|------|------|
| `package.json` | 신규 생성 |
| `.husky/pre-commit` | 신규 생성 |
| `.gitignore` | `node_modules/` 추가 |
| `CONTRIBUTING.md` | Git Hooks 섹션 추가 |
| `CLAUDE.md` | Pre-commit hook 설명 추가 |

**수정하지 않는 파일**: `build.gradle` (기존 checkstyleDiff 그대로 활용), CI 워크플로우 (전체 검사 유지)

## 검증 방법

1. `npm install` 실행 → `.husky/` 디렉토리 생성 확인
2. Java 파일에 의도적 스타일 위반 추가 → `git add` → `git commit` → 커밋 차단 확인
3. 위반 수정 → 커밋 성공 확인
4. Java 외 파일만 커밋 → hook이 checkstyle 건너뛰는지 확인
5. `./gradlew build` — 기존 빌드에 영향 없음 확인
