# Process Models

Содержит BPMN/DMN модели, которые деплоятся отдельно от бизнес‑сервисов.

Структура:
- `bpmn/` — процессы (BPMN)
- `dmn/` — таблицы решений (DMN)

Локальный деплой:
- `powershell -ExecutionPolicy Bypass -File scripts/deploy-camunda-models.ps1`
- `bash scripts/deploy-camunda-models.sh`

CI:
- Переменные: `CAMUNDA_BASE_URL`, опционально `CAMUNDA_USER`/`CAMUNDA_PASSWORD`.
