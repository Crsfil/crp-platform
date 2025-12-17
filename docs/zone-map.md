# Карта зоны ответственности (Inventory / Procurement / Reports / Auth)

Эта “карта” — короткий навигатор по твоей зоне: **эндпоинты → бизнес‑операции → права → события → хранилища**.
Её удобно использовать для:
- онбординга (быстро понять “что где живёт”);
- ревью изменений (видно, какие события/эндпоинты затрагиваются);
- планирования enterprise‑доработок (видно, каких частей не хватает).

## Как пользоваться
1) Выбираешь бизнес‑сценарий (например, “изъятие → хранение → реализация”).
2) Идёшь в раздел **Inventory API** и смотришь операции (transfer/status/lease/repossess/disposition).
3) Проверяешь права (PreAuthorize / trusted‑clients) и события Kafka (раздел “События”).
4) Смотришь, где лежат файлы (S3/MinIO) и какие переменные окружения нужны.

## Inventory API (inventory-service)
Ключевые контроллеры:
- `inventory-service/src/main/java/com/example/crp/inventory/web/EquipmentLifecycleController.java:1`
- `inventory-service/src/main/java/com/example/crp/inventory/web/LocationsController.java:1`
- `inventory-service/src/main/java/com/example/crp/inventory/web/StocktakeController.java:1`

**Операции с активом (не CRUD):**
- `POST /equipment/{id}/transfer` — перевод актива между локациями + запись движения + outbox событие.
- `POST /equipment/{id}/status` — смена статуса по допустимым переходам (`EquipmentStatus.allowedNext`) + история статусов + outbox событие.
- `GET /equipment/{id}/movements` — последние движения.
- `GET /equipment/{id}/status-history` — история статусов.

**Паспорт/поиск/документы:**
- `PATCH /equipment/{id}` — обновление “паспорта” (инв.номер/серийник/производитель/локация/ответственный).
- `GET /equipment/search` — поиск по status/location/responsible/inventoryNumber/serial/manufacturer/type/price.
- `GET /equipment/{equipmentId}/documents` / `POST /equipment/{equipmentId}/documents` — список и загрузка документов (FS или S3).
- `GET /equipment/documents/{id}/download` / `GET /equipment/documents/{id}/download-url` — скачивание / presign.

**Лизинг (lease lifecycle):**
- `POST /equipment/{id}/lease/start` — выдача в лизинг (transfer + status=LEASED + запись lease).
- `POST /equipment/{id}/lease/return` — возврат (transfer + status=RETURNED + финальный статус, обычно IN_STORAGE).
- `POST /equipment/{id}/lease/repossess` — изъятие (transfer + status=REPOSSESSED).
- `GET /equipment/{id}/lease/active` / `GET /equipment/{id}/leases` — активный лизинг/история.

**Осмотры/ремонты/реализация:**
- Осмотр: `POST /equipment/{id}/inspections` → submit → approve → complete/cancel (+ findings, docs).
- Ремонт: `POST /equipment/{id}/repairs` → approve → start → complete/cancel (+ lines, docs).
- Реализация/утилизация: `POST /equipment/{id}/dispositions` → approve → (sale contract/invoice/paid) → complete/cancel (+ docs).

**Инвентаризация (stocktake):**
- `POST /inventory/stocktakes` — создать инвентаризацию по локации (сгенерирует строки по текущим активам в локации).
- `POST /inventory/stocktakes/{id}/count` — посчитать строку (present/location/status/note).
- `POST /inventory/stocktakes/{id}/submit` — отправить.
- `POST /inventory/stocktakes/{id}/close?apply=true|false` — закрыть (и при apply=true применит transfer/status на активы).

**Права/безопасность:**
- запись: `INVENTORY_WRITE` или `ROLE_ADMIN`;
- чтение: `INVENTORY_READ` или `ROLE_ADMIN`, либо trusted client (`crp.security.trusted-clients`) для внутренних read‑вызовов.
- ABAC по локациям (опционально): `inventory.security.location-abac.enabled` (claim по умолчанию `region`).

## Procurement API (procurement-service)
Ключевые контроллеры:
- `procurement-service/src/main/java/com/example/crp/procurement/web/ProcurementController.java:1`
- `procurement-service/src/main/java/com/example/crp/procurement/web/PurchaseOrdersController.java:1`
- `procurement-service/src/main/java/com/example/crp/procurement/web/GoodsReceiptsController.java:1`

**ЖЦ заявки на закупку (request):**
- `POST /requests` — создать заявку (с линиями), статус `SUBMITTED`, outbox `procurement.requested`.
- `PATCH /requests/{id}/approve` — утвердить, статус `APPROVED`, outbox `procurement.approved` (триггерит резервацию в inventory).
- `PATCH /requests/{id}/reject` — отклонить, статус `REJECTED`, outbox `procurement.rejected` (триггерит release в inventory).
- `POST /requests/{id}/purchase-orders` — создать PO из APPROVED заявки (меняет статус заявки на `PO_CREATED`).

**PO и приёмка:**
- `POST /purchase-orders` — создать PO из заявки.
- `POST /purchase-orders/{id}/send` — отправить поставщику.
- `POST /purchase-orders/{id}/receipts` — оформить приёмку (может быть partial).
- `POST /receipts/{id}/accept` — принять (публикует `procurement.goods_accepted`, из него inventory создаёт Equipment).

**Поставщики и вложения:**
- `GET/POST /suppliers`, `PATCH /suppliers/{id}/status` (admin).
- `GET/POST /attachments/{ownerType}/{ownerId}` + download/presign.
- `POST /service/rfq` / `GET /service/rfq` / `POST /service/rfq/{id}/offers` / `POST /service/rfq/{id}/award` — RFQ/тендер на услуги (эвакуация/хранение/оценка/ремонт/аукцион).
- `POST /service/orders` / `GET /service/orders` / `POST /service/orders/{id}/complete` — сервисные ордера под услуги с актами/стоимостью (публикуют `procurement.service_completed`).

**Права/безопасность:**
- чтение: `PROCUREMENT_READ` или `ROLE_ADMIN` (часть эндпоинтов также разрешает trusted client для чтения);
- запись: `PROCUREMENT_WRITE` или `ROLE_ADMIN`;
- approve: `PROCUREMENT_APPROVE` или `ROLE_ADMIN`.

## Reports API (reports-service)
Ключевые контроллеры:
- `reports-service/src/main/java/com/example/crp/reports/web/ReportsController.java:1` (sync XLSX)
- `reports-service/src/main/java/com/example/crp/reports/web/ReportJobsController.java:1` (async jobs)

**Sync (скачать сразу):**
- `GET /reports/equipment-by-status.xlsx`
- `GET /reports/requests.xlsx`
- `GET /reports/procurement-pipeline.xlsx`
- `GET /reports/supplier-spend.xlsx`
- `GET /reports/repossessed-portfolio.xlsx`
- `GET /reports/storage-costs.xlsx`
- `GET /reports/disposition-results.xlsx`

**Async jobs (Quartz):**
- `POST /report-jobs/*` — создать job (PENDING) → Quartz выполнит генерацию → `DONE`/`FAILED`.
- `GET /report-jobs/{id}` — статус.
- `GET /report-jobs/{id}/download` / `download-url` — скачать/получить presign.
- Новые джобы: `/report-jobs/repossessed-portfolio`, `/report-jobs/storage-costs`.
- Ещё: `/report-jobs/disposition-results`.

**Шаблоны отчётов:**
- `POST /report-templates` (multipart) — загрузка шаблона в S3/FS (роль `REPORTS_ADMIN`).
- `GET /report-templates` / `/{id}` / `/{id}/download` / `/{id}/download-url`.

**S2S доступ к другим сервисам:**
- WebClient с `client_credentials` через `service-auth-client` (`S2SClientsConfig`).
- Требование аудитории `aud` включено в ресурс‑сервисах (`crp.security.jwt.enforce-audience=true`), поэтому Keycloak должен добавлять audience‑mapper для каждого целевого сервиса.

## События (Kafka topics)
**Procurement → Inventory:**
- `procurement.approved` / `procurement.rejected` — inventory резервирует/освобождает актив (`InventoryReservationService`).
- `procurement.goods_accepted` — inventory создаёт Equipment по строкам приёмки (`InboundReceiptIngestionService`).

**Inventory → Procurement:**
- `inventory.reserved` / `inventory.released` / `inventory.reserve_failed` — procurement обновляет статус заявки (RESERVED/REJECTED/FAILED).

**Inventory (outbox доменные события):**
`inventory.equipment.*`, `inventory.location.created`, `inventory.stocktake.*` и т.п. (см. `inventory-service/src/main/java/com/example/crp/inventory/messaging/Events.java:1`).

## Инфраструктура (где что лежит)
- Docker Compose: `docker-compose.yml` — Keycloak (realm импорт из `infrastructure/keycloak/realm-export`), Kafka (3 брокера), Redis, MinIO (S3), Postgres на сервис, observability (Grafana/Prometheus/Loki/Tempo/Alloy).
- K8s манифесты: `infrastructure/k8s/` (namespace/infra/postgres/observability/сервисы). Скрипт для minikube: `scripts/k8s-up.ps1`.
- S3/MinIO: бакеты создаёт `minio-init` (env `REPORTS_S3_BUCKET`, `PROCUREMENT_S3_BUCKET`, `INVENTORY_S3_BUCKET`). Переменные сервисов: `REPORTS_STORAGE_TYPE`/`REPORTS_S3_*`, `PROCUREMENT_ATTACHMENTS_STORAGE_TYPE`/`PROCUREMENT_S3_*`, `INVENTORY_DOCS_STORAGE_TYPE`/`INVENTORY_S3_*`.
- Kafka: `KAFKA_BOOTSTRAP` по умолчанию `kafka1:9092,kafka2:9092,kafka3:9092` (под docker-compose). Топики создаются авто при публикации через outbox.
- БД: Postgres на сервис (`inventory-postgres`, `procurement-postgres`, `reports-postgres` и т.д.); доступы задаются в compose (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD` в `application.yml` каждого сервиса).
- Observability: конфиги в `infrastructure/observability/` (Prometheus scrape, Loki/Tempo/Alloy, Grafana provisioning). Поднимается автоматически с docker compose.
