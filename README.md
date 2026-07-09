# Каркас синхронизации RAG-документации

Этот репозиторий — переносимый каркас для поддержания согласованности между сгенерированной Markdown-документацией и внешней RAG-платформой. В нём есть две независимые части:

- `git-hooks/` содержит локальный pre-commit hook. Он определяет, когда нужно вызвать генератор документации.
- Gradle-проект в корне — это модуль `rag-sync-service`. Он принимает события о commit'ах, создаёт асинхронные задания и синхронизирует Markdown через интерфейсы.

Изначально репозиторий был единым Gradle-проектом, поэтому сервис расположен в корне, а не во вложенной и избыточной директории `rag-sync-service/`. Здесь намеренно нет настоящих интеграций с Git-провайдером, RAG-платформой, корпоративных URL, токенов или agent skill. `StubGitProviderClient` и `StubRagPlatformClient` — безопасные заменители.

## Как это работает

1. Разработчик меняет исходный код и делает `git commit`.
2. Hook анализирует только файлы в staged area. Если они релевантны документации, он запускает настроенную внешнюю команду генерации.
3. Если генератор изменил `docs/**/*.md`, hook прерывает commit. Разработчик просматривает Markdown, вручную добавляет его через `git add` и повторяет commit.
4. После попадания commit в default branch Git job или webhook вызывает HTTP API сервиса.
5. Контроллер сохраняет job и сразу отвечает `202 Accepted`; тяжёлая работа выполняется фоновым worker'ом.
6. Worker читает изменения Markdown через `GitProviderClient`, обновляет или удаляет документы через `RagPlatformClient`, затем сохраняет состояние синхронизации в БД.

Markdown в `docs/` default branch — единственный источник истины. RAG-индекс и БД сервиса являются производными данными, которые можно восстановить из репозитория.

## Быстрый старт

Установите и настройте локальный hook:

```bash
cp git-hooks/.rag-docs-hook.env.example .rag-docs-hook.env
./git-hooks/scripts/rag-docs/install-hooks.sh
```

Для безопасной локальной проверки измените `.rag-docs-hook.env` и включите пример генератора:

```bash
RAG_DOCS_GENERATE_COMMAND='git-hooks/scripts/rag-docs/generate-rag-docs.example.sh'
```

Запустите сервис с H2:

```bash
./gradlew bootRun
```

Сервис слушает порт `8080`; локальный токен разработки — `local-dev-token`. Чтобы запустить dev-стек с PostgreSQL, сначала соберите boot jar:

```bash
./gradlew bootJar
docker compose up --build
```

## Замена заглушек

Реализуйте production-версии `GitProviderClient` (например, `GitLabGitProviderClient`) и `RagPlatformClient`, затем удалите или отключите соответствующие stub-bean. Настройте регистрацию репозиториев, авторизацию, PostgreSQL, блокировки, метрики и настоящий вызов agent skill по [руководству по доработке](docs/extension-guide.md).

## Документация

- [Архитектура](docs/architecture.md)
- [Установка hook и контракт генератора](docs/git-hook.md)
- [API и жизненный цикл сервиса](docs/rag-sync-service.md)
- [Руководство по production-доработке](docs/extension-guide.md)

## Необязательный пример GitLab job

Не добавляйте этот фрагмент как обязательный CI-файл. В целевом GitLab-проекте job может уведомлять сервис после попадания изменений документации в default branch:

```yaml
rag_docs_sync_notify:
  stage: post
  rules:
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
      changes:
        - docs/**/*.md
  allow_failure: true
  script:
    - |
      curl -X POST "$RAG_SYNC_URL/api/v1/rag-sync/events/git" \
        -H "X-Rag-Sync-Token: $RAG_SYNC_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
          \"projectId\": \"$CI_PROJECT_ID\",
          \"projectPath\": \"$CI_PROJECT_PATH\",
          \"branch\": \"$CI_COMMIT_BRANCH\",
          \"beforeSha\": \"$CI_COMMIT_BEFORE_SHA\",
          \"afterSha\": \"$CI_COMMIT_SHA\"
        }"
```
