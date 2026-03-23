---
name: commit:commit-push-pr
allowed-tools:
  - Bash(git *)
  - Bash(gh pr create:*)
description: Commit, push, and open a PR. Args: [main] to target main branch (default: develop)
argument-hint: "[main]"
---

## Context

- Current git status: !`git status`
- Current git diff (staged and unstaged changes): !`git diff HEAD`
- Current branch: !`git branch --show-current`

## Your task

Based on the above changes:
1. If on `main` or `develop`, create a new branch with an appropriate name (e.g., `feature/<short-description>`, `fix/<short-description>`, `refactor/<short-description>`)
2. Create a single commit with an appropriate message following the convention: `<type>: <한글 설명>`
3. Push the branch to origin
4. Create a pull request using `gh pr create`:
   - Default base branch: `develop`
   - If the user passed `main` as an argument, use `--base main` instead
5. You have the capability to call multiple tools in a single response. You MUST do all of the above in a single message. Do not use any other tools or do anything else. Do not send any other text or messages besides these tool calls.
