#!/usr/bin/env bash
set -euo pipefail

# В целевом проекте замените этот скрипт настоящим вызовом agent skill.
# Пример:
# agent-cli run-skill generate-rag-docs \
#   --repo-root "$REPO_ROOT" \
#   --changed-files "$RAG_DOCS_CHANGED_FILES_FILE" \
#   --output-dir "$RAG_DOCS_OUTPUT_DIR"

: "${REPO_ROOT:?REPO_ROOT must be set by the hook}"
: "${RAG_DOCS_OUTPUT_DIR:?RAG_DOCS_OUTPUT_DIR must be set by the hook}"
: "${RAG_DOCS_CHANGED_FILES_FILE:?RAG_DOCS_CHANGED_FILES_FILE must be set by the hook}"

echo "Пример stub-генератора RAG"
echo "REPO_ROOT=$REPO_ROOT"
echo "RAG_DOCS_OUTPUT_DIR=$RAG_DOCS_OUTPUT_DIR"
echo "Релевантные изменённые файлы:"
sed 's/^/  - /' "$RAG_DOCS_CHANGED_FILES_FILE"

if [[ "$RAG_DOCS_OUTPUT_DIR" = /* ]]; then
  output_dir="$RAG_DOCS_OUTPUT_DIR"
else
  output_dir="$REPO_ROOT/$RAG_DOCS_OUTPUT_DIR"
fi
mkdir -p "$output_dir"
cat > "$output_dir/example-generated-doc.md" <<'EOF'
---
doc_id: example-generated-rag-document
---

# Пример сгенерированного RAG-документа

Этот файл создан безопасной локальной заглушкой. В целевом проекте замените её настоящим генератором документации.
EOF
