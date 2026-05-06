#!/bin/bash
# save-prompt.sh — UserPromptSubmit hook: persist prompt payload per session

set -euo pipefail

INPUT=$(cat)
SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // "unknown"')

MESSAGES_DIR="${CLAUDE_PROJECT_DIR:-.}/.claude/messages"
mkdir -p "$MESSAGES_DIR"

echo "$INPUT" | jq -c '.' >> "$MESSAGES_DIR/${SESSION_ID}.jsonl"

exit 0
