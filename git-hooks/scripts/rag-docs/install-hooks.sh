#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

if [[ ! -f .rag-docs-hook.env ]]; then
  cp git-hooks/.rag-docs-hook.env.example .rag-docs-hook.env
  echo "Создан .rag-docs-hook.env из примера. Настройте его до commit с релевантными изменениями."
else
  echo "Существующий .rag-docs-hook.env оставлен без изменений."
fi

git config core.hooksPath git-hooks/.githooks
chmod +x git-hooks/.githooks/pre-commit
chmod +x git-hooks/scripts/rag-docs/pre-commit-rag-docs.sh
chmod +x git-hooks/scripts/rag-docs/generate-rag-docs.example.sh
chmod +x git-hooks/scripts/rag-docs/run-agent-skill.example.sh

echo "Pre-commit hook для RAG-документации успешно установлен."
