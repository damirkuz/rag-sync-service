#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

if [[ -f "$repo_root/.rag-docs-hook.env" ]]; then
  # shellcheck disable=SC1091
  source "$repo_root/.rag-docs-hook.env"
fi

: "${RAG_DOCS_OUTPUT_DIR:=docs}"
: "${RAG_DOCS_RELEVANT_PATH_REGEX:=^(src/main/(kotlin/.*\\.kt|java/.*\\.java|resources/.*\\.(bpmn|dmn)|resources/application[^/]*\\.ya?ml)|build\\.gradle\\.kts|settings\\.gradle\\.kts|pom\\.xml|docs/rag-config/|rag-docs-config/|\\.rag-docs-hook\\.env$)}"

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/rag-docs-hook.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT
staged_files_file="$tmp_dir/staged-files.txt"
changed_files_file="$tmp_dir/changed-files.txt"
before_snapshot="$tmp_dir/output-before.txt"
after_snapshot="$tmp_dir/output-after.txt"

git diff --cached --name-only --diff-filter=ACMR > "$staged_files_file"
if [[ ! -s "$staged_files_file" ]]; then
  exit 0
fi

set +e
grep -E -- "$RAG_DOCS_RELEVANT_PATH_REGEX" "$staged_files_file" > "$changed_files_file"
grep_status=$?
set -e
if [[ $grep_status -eq 2 ]]; then
  echo "RAG docs hook: RAG_DOCS_RELEVANT_PATH_REGEX не является корректным расширенным регулярным выражением." >&2
  exit 1
fi
if [[ ! -s "$changed_files_file" ]]; then
  exit 0
fi

unstaged_relevant="$tmp_dir/unstaged-relevant.txt"
while IFS= read -r path; do
  if ! git diff --quiet -- "$path"; then
    printf '%s\n' "$path" >> "$unstaged_relevant"
  fi
done < "$changed_files_file"

if [[ -s "$unstaged_relevant" ]]; then
  echo "RAG docs hook остановил commit: в этих релевантных файлах одновременно есть staged- и unstaged-изменения:" >&2
  sed 's/^/  - /' "$unstaged_relevant" >&2
  echo "Добавьте все изменения этих файлов в staged area или временно уберите unstaged-изменения, затем повторите попытку." >&2
  echo "Hook не создаёт stash, потому что генератор читает рабочую директорию." >&2
  exit 1
fi

if [[ -z "${RAG_DOCS_GENERATE_COMMAND:-}" ]]; then
  echo "RAG docs hook остановил commit: RAG_DOCS_GENERATE_COMMAND не настроена." >&2
  echo "Создайте .rag-docs-hook.env из git-hooks/.rag-docs-hook.env.example и настройте генератор." >&2
  echo "Пример: RAG_DOCS_GENERATE_COMMAND='git-hooks/scripts/rag-docs/generate-rag-docs.example.sh'" >&2
  exit 1
fi

if [[ "$RAG_DOCS_OUTPUT_DIR" = /* ]]; then
  output_dir_absolute="$RAG_DOCS_OUTPUT_DIR"
else
  output_dir_absolute="$repo_root/$RAG_DOCS_OUTPUT_DIR"
fi

snapshot_markdown() {
  local output_file="$1"
  : > "$output_file"
  [[ -d "$output_dir_absolute" ]] || return 0
  while IFS= read -r -d '' file; do
    local digest path
    digest="$(shasum -a 256 "$file" | awk '{print $1}')"
    path="${file#"$repo_root"/}"
    printf '%s %s\n' "$digest" "$path" >> "$output_file"
  done < <(find "$output_dir_absolute" -type f -name '*.md' -print0)
  LC_ALL=C sort -o "$output_file" "$output_file"
}

snapshot_markdown "$before_snapshot"
export REPO_ROOT="$repo_root"
export RAG_DOCS_OUTPUT_DIR
export RAG_DOCS_CHANGED_FILES_FILE="$changed_files_file"
export RAG_DOCS_STAGED_FILES_FILE="$staged_files_file"

echo "RAG docs hook: запускается настроенный генератор документации для $(wc -l < "$changed_files_file" | tr -d ' ') релевантных staged-файлов."
bash -lc "$RAG_DOCS_GENERATE_COMMAND"
snapshot_markdown "$after_snapshot"

if ! cmp -s "$before_snapshot" "$after_snapshot"; then
  echo "RAG-документация обновлена." >&2
  echo "Изменённые Markdown-файлы:" >&2
  (comm -3 "$before_snapshot" "$after_snapshot" || true) | awk '{print "  - " $2}' | sort -u >&2
  echo "Проверьте сгенерированную документацию." >&2
  echo "Добавьте документацию в staged area: git add $RAG_DOCS_OUTPUT_DIR" >&2
  echo "Затем повторите commit." >&2
  exit 1
fi
