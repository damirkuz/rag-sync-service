#!/usr/bin/env bash
set -euo pipefail

# Эта обёртка намеренно не реализует настоящий генератор документации.
# В целевом проекте задайте в AGENT_SKILL_COMMAND настоящий вызов agent skill.

: "${REPO_ROOT:?REPO_ROOT must be set by the hook}"
: "${RAG_DOCS_OUTPUT_DIR:?RAG_DOCS_OUTPUT_DIR must be set by the hook}"
: "${RAG_DOCS_CHANGED_FILES_FILE:?RAG_DOCS_CHANGED_FILES_FILE must be set by the hook}"
: "${RAG_DOCS_STAGED_FILES_FILE:?RAG_DOCS_STAGED_FILES_FILE must be set by the hook}"

if [[ ! -f "$RAG_DOCS_CHANGED_FILES_FILE" || ! -f "$RAG_DOCS_STAGED_FILES_FILE" ]]; then
  echo "Обёртка agent skill: входные файлы changed-files и staged-files должны существовать." >&2
  exit 1
fi

echo "Обёртка agent skill: релевантные изменённые файлы:"
sed 's/^/  - /' "$RAG_DOCS_CHANGED_FILES_FILE"

if [[ "$RAG_DOCS_OUTPUT_DIR" = /* ]]; then
  output_dir="$RAG_DOCS_OUTPUT_DIR"
else
  output_dir="$REPO_ROOT/$RAG_DOCS_OUTPUT_DIR"
fi
mkdir -p "$output_dir"

if [[ -z "${AGENT_SKILL_COMMAND:-}" ]]; then
  echo "AGENT_SKILL_COMMAND не настроена." >&2
  echo "Задайте её в .rag-docs-hook.env, например:" >&2
  echo "AGENT_SKILL_COMMAND='agent-cli run-skill generate-rag-docs --repo-root \"\$REPO_ROOT\" --changed-files \"\$RAG_DOCS_CHANGED_FILES_FILE\" --staged-files \"\$RAG_DOCS_STAGED_FILES_FILE\" --output-dir \"\$RAG_DOCS_OUTPUT_DIR\"'" >&2
  exit 1
fi

bash -lc "$AGENT_SKILL_COMMAND"
