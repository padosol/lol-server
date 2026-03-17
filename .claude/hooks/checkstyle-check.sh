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

# Parse: violations appear as "[ERROR] file:line:col: message"
VIOLATIONS=$(echo "$OUTPUT" | grep -E '^\[ERROR\]|^\[WARN\]' || true)

if [[ -n "$VIOLATIONS" ]]; then
  echo "Checkstyle 위반 발견:" >&2
  echo "$VIOLATIONS" >&2
  exit 1
fi

exit 0
