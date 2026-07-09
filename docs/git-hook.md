# Git-хук pre-commit

Установите hook один раз для каждого клона репозитория:

```bash
cp git-hooks/.rag-docs-hook.env.example .rag-docs-hook.env
./git-hooks/scripts/rag-docs/install-hooks.sh
```

Установщик задаёт `core.hooksPath=git-hooks/.githooks`. Файл `pre-commit` намеренно остаётся тонкой обёрткой: вся логика находится в `scripts/rag-docs/pre-commit-rag-docs.sh`.

## Режимы генератора

Для безопасной локальной проверки используйте stub-генератор:

```bash
RAG_DOCS_GENERATE_COMMAND='git-hooks/scripts/rag-docs/generate-rag-docs.example.sh'
```

Для agent skill целевого проекта используйте переносимую обёртку:

```bash
RAG_DOCS_GENERATE_COMMAND='git-hooks/scripts/rag-docs/run-agent-skill.example.sh'

AGENT_SKILL_COMMAND='agent-cli run-skill generate-rag-docs \
  --repo-root "$REPO_ROOT" \
  --changed-files "$RAG_DOCS_CHANGED_FILES_FILE" \
  --staged-files "$RAG_DOCS_STAGED_FILES_FILE" \
  --output-dir "$RAG_DOCS_OUTPUT_DIR"'
```

Название команды выше приведено только как пример. `pre-commit-rag-docs.sh` сам не генерирует документацию: он определяет, изменились ли релевантные staged-файлы, и запускает `RAG_DOCS_GENERATE_COMMAND`. Обёртка тоже не содержит генератор или нейросеть: она проверяет входные данные и запускает `AGENT_SKILL_COMMAND`. Настоящий agent skill должен создать или обновить Markdown в `RAG_DOCS_OUTPUT_DIR`.

Hook экспортирует `REPO_ROOT`, `RAG_DOCS_OUTPUT_DIR`, `RAG_DOCS_CHANGED_FILES_FILE` и `RAG_DOCS_STAGED_FILES_FILE`. Оба файла со списками содержат пути относительно корня репозитория; список changed-файлов — отфильтрованное подмножество staged-файлов.

## Сценарий commit

Hook сопоставляет staged additions, copies, modifications и renames с настраиваемым регулярным выражением. По умолчанию оно охватывает Kotlin/Java-исходники, BPMN/DMN, application YAML, Gradle/Maven-дескрипторы, директории конфигурации RAG и `.rag-docs-hook.env`.

Если в релевантном файле одновременно есть staged- и unstaged-правки, hook останавливает commit до запуска генератора. Генератор читает рабочую директорию, поэтому смешение состояний могло бы задокументировать код, который не попадёт в commit. Добавьте все правки такого файла в staged area либо временно уберите unstaged-правки; hook никогда не делает stash автоматически.

Если генератор изменил Markdown, hook останавливает commit и выводит список результата. Просмотрите изменения, затем выполните:

```bash
git add docs
git commit
```

Auto-stage намеренно отсутствует: сгенерированная документация должна быть явно проверяемой частью commit. `git commit --no-verify` временно обходит hook, но использовать его не рекомендуется: документация в default branch может устареть.
