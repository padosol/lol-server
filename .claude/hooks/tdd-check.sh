#!/usr/bin/env bash
# TDD Enforcement Hook for Claude Code
# Blocks Edit/Write on production Java files unless the corresponding test file
# has already been modified in the current git working tree.

set -euo pipefail

# Read tool input JSON from stdin
INPUT=$(cat)

# Extract file_path from the JSON payload
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.filePath // empty' 2>/dev/null)

# If no file path found, allow (non-file tool call)
if [[ -z "$FILE_PATH" ]]; then
  exit 0
fi

# Only check .java files under src/main/java
if [[ "$FILE_PATH" != *"src/main/java"* ]] || [[ "$FILE_PATH" != *.java ]]; then
  exit 0
fi

# Allow editing test files themselves (*Test.java)
BASENAME=$(basename "$FILE_PATH" .java)
if [[ "$BASENAME" == *Test ]]; then
  exit 0
fi

# Skip if not in a git repository
if ! git rev-parse --is-inside-work-tree &>/dev/null; then
  exit 0
fi

# Calculate the corresponding test file path
# src/main/java/.../Foo.java -> src/test/java/.../FooTest.java
TEST_PATH="${FILE_PATH/src\/main\/java/src\/test\/java}"
TEST_PATH="${TEST_PATH%.java}Test.java"

# Check if the test file has been modified (staged or unstaged) in git
MODIFIED_FILES=$(git status --porcelain 2>/dev/null | awk '{print $NF}')

# Resolve to absolute path for comparison
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)

# Make TEST_PATH relative to repo root for comparison
if [[ "$TEST_PATH" == /* ]]; then
  REL_TEST_PATH="${TEST_PATH#$REPO_ROOT/}"
else
  REL_TEST_PATH="$TEST_PATH"
fi

# Also try matching by just the filename pattern in case of multi-module paths
TEST_FILENAME=$(basename "$TEST_PATH")

# Check if test file appears in modified files
FOUND=false
while IFS= read -r modified; do
  [[ -z "$modified" ]] && continue
  # Match by relative path
  if [[ "$modified" == *"$REL_TEST_PATH"* ]]; then
    FOUND=true
    break
  fi
  # Match by filename (for multi-module projects where paths may differ)
  if [[ "$(basename "$modified")" == "$TEST_FILENAME" ]]; then
    FOUND=true
    break
  fi
done <<< "$MODIFIED_FILES"

if [[ "$FOUND" == "false" ]]; then
  echo "TDD VIOLATION: 프로덕션 코드를 수정하기 전에 테스트를 먼저 작성하세요!" >&2
  echo "" >&2
  echo "  프로덕션 파일: $FILE_PATH" >&2
  echo "  필요한 테스트: $TEST_PATH" >&2
  echo "" >&2
  echo "테스트 파일을 먼저 작성/수정한 후 다시 시도하세요." >&2
  echo "TDD 원칙: RED (실패하는 테스트) -> GREEN (최소 구현) -> REFACTOR" >&2
  exit 2
fi

exit 0
