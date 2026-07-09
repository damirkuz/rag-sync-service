# Сервис синхронизации RAG

Запустите каркас командой `./gradlew bootRun`. По умолчанию используется H2; для настоящей PostgreSQL выберите профиль `postgres`. Все запросы к `/api/v1/rag-sync/**` требуют заголовок `X-Rag-Sync-Token`. Значение `local-dev-token` подходит только для локальной разработки.

## API

`POST /api/v1/rag-sync/events/git` принимает уведомление о новом commit и возвращает `202 Accepted`:

```json
{"projectId":"12345","projectPath":"backend/payment-service","branch":"master","beforeSha":"old","afterSha":"new"}
```

Ответ:

```json
{"status":"accepted","jobId":"uuid"}
```

`GET /api/v1/rag-sync/jobs/{jobId}` возвращает статус job, проект и branch, SHA, временные метки и сообщение об ошибке, если оно есть. `POST /api/v1/rag-sync/reconcile` создаёт job типа `RECONCILE` с полями `projectId`, `projectPath`, `branch` и `commitSha`.

## Хранение данных и жизненный цикл

`rag_repositories` хранит конфигурацию проекта; в каркасе запись создаётся при первом событии. `rag_sync_jobs` хранит идемпотентные job (уникальны репозиторий, branch и целевой SHA). `rag_sync_state` запоминает последний полностью успешный commit. `rag_documents` связывает исходные пути, doc ID и хеши с идентификаторами документов во внешней RAG-платформе.

Для веток, не являющихся default branch, создаются job со статусом `IGNORED`. Планировщик переводит job из `PENDING` в `PROCESSING`, а затем в `SUCCESS` или `FAILED`; при захвате job счётчик попыток увеличивается. Ошибка никогда не продвигает состояние синхронизации.

Для incremental job `fromSha` — сохранённый SHA последнего успеха либо `beforeSha` события. Изменения из `GitProviderClient` передаются в `RagPlatformClient` для upsert/delete; переименование в каркасе обрабатывается как delete старого пути и add нового. Reconcile получает все подходящие Markdown-пути, добавляет отсутствующие или изменённые файлы и удаляет записи, которых больше нет в Git.

`DocIdService` использует `doc_id` из YAML frontmatter, если поле задано; иначе формирует `projectPath:branch:path`. `ContentHashService` считает SHA-256 после нормализации LF и пробелов в конце строк. Каждый upsert передаёт метаданные проекта, ветки, commit, пути, doc ID, хеша и `sourceType=generated-md`.

## Заглушки интеграций

`StubGitProviderClient` не возвращает файлов и не делает сетевых запросов. `StubRagPlatformClient` логирует операции и возвращает фиктивный удалённый ID. Благодаря этому сервис безопасно запускать до подключения реальных адаптеров.
