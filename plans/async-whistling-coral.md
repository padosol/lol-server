# PostToolUse Checkstyle Hook

## Context
Edit/Write 도구로 `.java` 파일을 수정할 때마다 즉시 checkstyle 위반을 피드백받고 싶다. Gradle 전체 빌드(~15s)는 hook으로 쓰기엔 느리므로, checkstyle CLI jar를 직접 실행하여 **편집된 파일 1개만** 빠르게 검사하는 PostToolUse hook을 만든다.

## 수정 파일

### 1. `.claude/hooks/checkstyle-check.sh` (신규)

PostToolUse hook 스크립트. 기존 `tdd-check.sh`와 동일한 패턴(stdin JSON → jq 파싱)을 따른다.

**동작 흐름:**
1. stdin에서 `tool_input.file_path` 추출 (jq 사용)
2. `.java` 파일이 아니면 → exit 0 (skip)
3. 제외 패턴 검사: `build/generated/`, `Q*.java`, `*MapperImpl.java`, `*ClientMapperImpl.java` → exit 0
4. Checkstyle CLI jar가 없으면 Maven Central에서 다운로드 (`config/checkstyle/checkstyle-10.21.4-all.jar`)
5. suppressionFile 프로퍼티를 동적 생성하여 `java -jar checkstyle.jar -c checkstyle.xml -p <props> <file>` 실행
6. 위반 없으면 exit 0, 위반 있으면 위반 내용 출력 + exit 1 (Claude에게 피드백)

```bash
#!/usr/bin/env bash
set -euo pipefail

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.filePath // empty' 2>/dev/null)

# Skip: no file path or not a .java file
[[ -z "$FILE_PATH" ]] && exit 0
[[ "$FILE_PATH" != *.java ]] && exit 0

# Skip: generated files
BASENAME=$(basename "$FILE_PATH")
[[ "$FILE_PATH" == *"build/generated"* ]] && exit 0
[[ "$BASENAME" == Q*.java ]] && exit 0
[[ "$BASENAME" == *MapperImpl.java ]] && exit 0
[[ "$BASENAME" == *ClientMapperImpl.java ]] && exit 0

# Skip: file doesn't exist (deleted)
[[ ! -f "$FILE_PATH" ]] && exit 0

# Resolve project root
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
CHECKSTYLE_DIR="$REPO_ROOT/config/checkstyle"
JAR="$CHECKSTYLE_DIR/checkstyle-10.21.4-all.jar"
CONFIG="$CHECKSTYLE_DIR/checkstyle.xml"
SUPPRESSIONS="$CHECKSTYLE_DIR/suppressions.xml"

# Download jar if missing
if [[ ! -f "$JAR" ]]; then
  curl -sSL -o "$JAR" \
    "https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.21.4/checkstyle-10.21.4-all.jar"
fi

# Create temp properties file for suppressionFile
PROPS=$(mktemp)
trap 'rm -f "$PROPS"' EXIT
echo "suppressionFile=$SUPPRESSIONS" > "$PROPS"

# Run checkstyle
OUTPUT=$(java -jar "$JAR" -c "$CONFIG" -p "$PROPS" "$FILE_PATH" 2>&1) || true

# Parse: checkstyle outputs "Starting audit..." and "Audit done." lines
# Violations appear between them as "[ERROR] file:line:col: message"
VIOLATIONS=$(echo "$OUTPUT" | grep -E '^\[ERROR\]|^\[WARN\]' || true)

if [[ -n "$VIOLATIONS" ]]; then
  echo "Checkstyle 위반 발견:" >&2
  echo "$VIOLATIONS" >&2
  exit 1
fi

exit 0
```

### 2. `.claude/settings.json` (수정)

`hooks` 필드에 PostToolUse 등록:

```json
{
  "enabledPlugins": {},
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": ".claude/hooks/checkstyle-check.sh"
          }
        ]
      }
    ]
  },
  "plansDirectory": "./plans"
}
```

### 3. `.gitignore` (수정)

checkstyle CLI jar 제외 추가:

```
config/checkstyle/checkstyle-*-all.jar
```

## 기존 파일과의 관계

| 파일 | 역할 |
|------|------|
| `tdd-check.sh` | PreToolUse - 테스트 선행 강제 (기존, 변경 없음) |
| `checkstyle-check.sh` | PostToolUse - 편집 후 즉시 스타일 검사 (신규) |
| `build.gradle` `-PcheckstyleDiff` | CI/수동 실행용 전체 diff 검사 (기존, 변경 없음) |

## 검증 (직접 테스트 방법)

### 테스트 1: 위반이 있는 Java 파일 Edit → hook이 에러 피드백하는지 확인

아무 `.java` 파일을 열어서 **의도적으로 checkstyle 위반**을 만든다. 쉽게 트리거할 수 있는 위반 예시:

| 위반 종류 | 방법 | 체크 모듈 |
|-----------|------|-----------|
| NeedBraces | `if (true) return;` (중괄호 없는 if) | NeedBraces |
| AvoidStarImport | `import java.util.*;` 추가 | AvoidStarImport |
| 줄 길이 초과 | 120자 초과 라인 작성 | LineLength |
| trailing whitespace | 줄 끝에 공백 추가 | RegexpSingleline |

**구체적 방법**: 임의의 Java 파일(예: `VersionService.java`)을 Edit으로 수정하여 `import java.util.*;` 한 줄을 추가한다. hook이 `[ERROR]` 메시지를 stderr로 출력하고 exit 1로 종료하면 성공.

### 테스트 2: 위반 없는 Java 파일 Edit → hook이 조용히 통과하는지 확인

정상적인 수정(예: 주석 추가)을 하고 hook이 exit 0으로 조용히 통과하는지 확인한다.

### 테스트 3: 비-Java 파일 Edit → skip 확인

`.md`, `.xml`, `.yml` 등 비-Java 파일을 Edit하고 hook이 즉시 skip(exit 0)하는지 확인한다.

### 테스트 4: 셸에서 직접 실행

plan mode 밖에서 다음 명령어로 직접 테스트 가능:

```bash
# 정상 파일 → exit 0
echo '{"tool_input":{"file_path":"/home/lol/lol-server/module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/version/application/VersionService.java"}}' \
  | .claude/hooks/checkstyle-check.sh && echo "PASS"

# 존재하지 않는 파일 → exit 0 (skip)
echo '{"tool_input":{"file_path":"/tmp/nonexistent.java"}}' \
  | .claude/hooks/checkstyle-check.sh && echo "PASS: skipped"

# 비-Java → exit 0 (skip)
echo '{"tool_input":{"file_path":"README.md"}}' \
  | .claude/hooks/checkstyle-check.sh && echo "PASS: skipped"
```

### 권장 테스트 순서

1. **테스트 4** (셸 직접 실행) → 스크립트 자체 동작 확인
2. **테스트 1** (위반 Edit) → hook 연동 + 에러 피드백 확인 (핵심)
3. **테스트 2** (정상 Edit) → false positive 없는지 확인
4. **테스트 3** (비-Java Edit) → 불필요한 실행 없는지 확인

테스트 1에서 의도적으로 넣은 위반은 테스트 후 즉시 원복한다.
