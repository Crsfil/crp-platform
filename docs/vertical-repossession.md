# Вертикаль «изъятие → хранение → реализация»

Цель: довести поток работы с проблемным активом до enterprise-уровня: фиксировать изъятие, безопасно хранить, оценивать/ремонтировать, реализовывать (продажа/утилизация), шить это с закупкой услуг подрядчиков и отчётами, с ролями/ABAC и аудитом.

Использование: как дизайн‑опора перед реализацией (миграции, API, события, Keycloak, отчёты).

## Поток (сквозной)
1) Триггер (просрочка/расторжение/решение риск-комитета) → `repossess` инициирован.
2) Эвакуация/доставка на площадку хранения (услуга) → статус IN_TRANSIT.
3) Приём на хранение (акт) → IN_STORAGE + custody (локация/ответственный).
4) Оценка (оценочная стоимость/ликвидационная стоимость, отчёт/фото) → UNDER_EVALUATION.
5) Ремонт/подготовка (при необходимости) → UNDER_REPAIR.
6) Листинг/аукцион → SALE_LISTED → SOLD | DISPOSED.
7) Финальное закрытие кейса, документы и события в отчёты.

## Данные и схемы (планируемые миграции)
Inventory (новые/расширенные таблицы):
- `equipment` — добавить `branch_code`, `current_custodian`, `repossession_case_id`, `valuation_amount`, `valuation_currency`, `valuation_at`.
- `equipment_status` — расширить статусы: REPOSSESSION_PENDING, IN_TRANSIT, IN_STORAGE, UNDER_EVALUATION, UNDER_REPAIR, SALE_LISTED, SOLD, DISPOSED (+ матрица переходов).
- `equipment_repossession_case` — id, equipment_id, trigger_reason, decision_ref, initiated_by, initiated_at, status (PENDING/IN_PROGRESS/CLOSED/CANCELED), correlation_id.
- `equipment_storage_order` — id, equipment_id, storage_location_id, vendor_id/name/inn, sla_until, started_at, released_at, expected_cost, actual_cost, currency, procurement_service_order_id.
- `equipment_valuation` — id, equipment_id, valuation_amount, liquidation_amount, currency, valuated_at, vendor_name/inn, report_doc_id (S3), note.
- `equipment_sale_listing` — id, equipment_id, method (DIRECT/AUCTION), lot_number, started_at, expires_at, reserve_price, currency, sale_price, buyer_name/inn, contract_number, invoice_number, paid_at.
- `equipment_custody_history` — equipment_id, location_id, custodian, from_ts, to_ts, reason (для trail по ответственности).

Procurement (новые поля/таблицы):
- `procurement_request.kind` — расширить значениями SERVICE_EVICTION, SERVICE_STORAGE, SERVICE_VALUATION, SERVICE_REPAIR, SERVICE_AUCTION.
- `procurement_service_order` — id, request_id, service_type, equipment_id, location_id, sla_until, vendor_id/name/inn, status (CREATED/IN_PROGRESS/COMPLETED/CANCELED), act_doc_id (S3), cost_planned/actual, currency.
- RFQ/тендер: `rfq` (id, title, service_type, equipment_id, location_id, status), `rfq_offer` (rfq_id, supplier_id, price, currency, eta, valid_until, status), `rfq_award` (rfq_id, supplier_id, reason, awarded_at).

Reports (артефактные таблицы под async jobs):
- `report_job` уже есть — добавить snapshot metadata (source_timestamp, dataset_version) и ссылку на template_id.
- Каталог шаблонов (можно S3 + запись в БД: template_id, path, version, params_schema).

## События Kafka (план)
- Inventory outbox: `inventory.repossession.started|stored|valuated|repair.started|repair.completed|sale.listed|sale.sold|disposed` (payload: equipmentId, status, locationId, valuationAmount/currency, custodian, correlationId, eventVersion).
- Procurement outbox: `procurement.service_awarded`, `procurement.service_completed` (serviceType, equipmentId, locationId, vendor, costActual, actDocumentId).
- Reports notifications: `reports.job.done` / `reports.job.failed` (jobId, templateId, params, snapshotTimestamp, downloadUrl).

## API (плановые расширения)
Inventory:
- `POST /equipment/{id}/repossessions` — открыть кейс (reason, decision_ref, target_location).
- `POST /equipment/{id}/storage` — принять на хранение (location, custodian, actDocument).
- `POST /equipment/{id}/valuation` — зафиксировать оценку (amount/liquidation/currency/vendor/doc).
- `POST /equipment/{id}/sale/list` — листинг (method, lot, reserve_price, expires_at).
- `POST /equipment/{id}/sale/close` — SOLD/DISPOSED (sale_price/buyer/contract/invoice/paid_at).
- `GET /equipment/{id}/custody` — история custody.
- Дополнить существующие `/dispositions` типами документов (evacuation_act, storage_act, valuation_report, auction_report).

Procurement:
- `POST /requests` с kind=SERVICE_* и ссылкой на equipment/location.
- `POST /rfq` / `POST /rfq/{id}/offers` / `POST /rfq/{id}/award`.
- `POST /service-orders/{id}/complete` с актом/стоимостью (публикует `procurement.service_completed`).

Reports:
- Sync/async: добавить шаблоны `repossessed-portfolio`, `storage-costs`, `disposition-results` (параметры: филиал/период/статусы).
- `POST /report-templates` (admin) — загрузка шаблона в S3 с metadata.

## Права и Keycloak
- Роли: `INVENTORY_REPOSSESS`, `INVENTORY_STORAGE`, `INVENTORY_DISPOSE`, `PROCUREMENT_TENDER`, `REPORTS_ADMIN`.
- ABAC по филиалу/локации: claim `branch`/`region`, маппинг в токен из групп Keycloak; проверки в Inventory/Procurement на операции repossess/storage/sale.
- MFA/step-up для операций: repossess, approve sale, award RFQ, close sale.
- Trusted clients (S2S): gateway, reports-service, procurement-service (для внутренних чтений) — audience мапперы должны быть в realm export.

## S3/файлы
- Типы документов: evacuation_act, storage_act, valuation_report, repair_report, auction_report, sale_contract, invoice, payment_confirmation, photo_set.
- Проверка MIME/размер, префиксы бакетов: `inventory/<equipmentId>/...`, `procurement/service-orders/<id>/...`, версии (ETag) для аудита.

## Observability/NFR
- Метрики: время цикла repossess→storage→sale, SLA хранения (storage_order.sla_until), SLA оценки, просрочки RFQ/award, Kafka outbox lag.
- Логи/трейсы: correlation_id обязательный; key business spans (repossess, valuation, sale close).
- Алёрты: просрочено SLA хранения/оценки/ремонта, зависшие статусы, проваленные отчётные джобы.

## Backlog (минимальный инкремент)
1) Inventory ✅: расширены статусы/allowedNext, добавлены `equipment_repossession_case`, `equipment_storage_order`, `equipment_valuation`, custody trail; эндпоинты repossess/storage/valuation; события outbox; обработка `procurement.service_completed` (storage/valuation/repair/auction).
2) Procurement ✅: сервисные order’ы и RFQ (сущности+эндпоинты) + событие `procurement.service_completed` в outbox + валидация kind=SERVICE_*.
3) Reports ✅/частично: sync отчёты `repossessed-portfolio`, `storage-costs` + async jobs; добавлены шаблоны в S3/FS и нотификации Kafka (осталось: `disposition-results`).
4) IAM ✅: роли/мэпперы/ABAC claim в Keycloak export; @PreAuthorize обновлён; MFA policy добавлена (TOTP required action).
5) Observability ✅/частично: Prometheus alerts + Grafana dashboard (осталось: SLA‑алерты по доменным метрикам).
6) Тесты ✅/частично: интеграционные/контрактные тесты добавлены (осталось: end‑to‑end с Kafka/MinIO при доступном Docker).
