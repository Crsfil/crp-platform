CRP Platform

Сервисы:
- auth-service — аутентификация и роли
- inventory-service — учет техники
- procurement-service — заявки на закупку
- reports-service — задания на отчеты
- customer-service — контрагенты
- kyc-service — проверка клиента (заглушка)
- underwriting-service — скоринг (заглушка)
- product-pricing-service — калькулятор графика
- application-service — заявки с оркестрацией KYC/UW/Pricing
- agreement-service — договоры (подписание)
- schedule-service — хранение графиков платежей
- billing-service — инвойсинг, авто-начисления из графика
- payments-service — вебхук платежей, идемпотентность (Redis) + Kafka
- accounting-service — простая бухгалтерия (проводки по инвойсам/платежам)
- gateway-service — API шлюз (единая точка входа, BFF)
- bpm-service — BPMN процесс заявки (Flowable)

Стек: Java 17, Spring Boot 3, Spring Security, JPA/Hibernate, Liquibase, PostgreSQL (на каждый сервис своя БД), Kafka, Redis, Actuator/Prometheus, OpenAPI.

Локальная инфраструктура (docker-compose): Keycloak, Kafka/Zookeeper, Redis, Postgres для сервисов, Prometheus/Grafana/Loki/Promtail. Kubernetes-манифесты в каталоге `k8s/` (адаптируйте `image` и хосты под кластер).

Быстрый старт (Docker, Keycloak + RS256):
- Запуск: `docker compose up -d --build` (в каталоге проекта)
- Поднимутся: Keycloak (порт 18080), Kafka+ZK, Redis, Postgres и все сервисы.
- Keycloak автоматически импортирует realm `crp` (пользователь `admin@crp.local`/`admin`, роли `ADMIN/MANAGER/ANALYST/USER`, клиент `crp-cli`).

Контуры по умолчанию (основные):
- gateway: http://localhost:8080
- auth: http://localhost:8081 (Swagger UI: /swagger-ui/index.html)
- inventory: http://localhost:8082
- procurement: http://localhost:8083
- reports: http://localhost:8084
- customer: http://localhost:8085
- kyc: http://localhost:8086
- underwriting: http://localhost:8087
- product-pricing: http://localhost:8088
- application: http://localhost:8089
- agreement: http://localhost:8090
- billing: http://localhost:8091
- payments: http://localhost:8092
- schedule: http://localhost:8093
- edocs: http://localhost:8094
- bpm: http://localhost:8095

Конфигурация окружения (ключевое):
- JWT ресурсы: `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` указывает на Issuer Keycloak (`http://keycloak:8080/realms/crp`). Симметричный `SECURITY_JWT_SECRET` не используется ресурс‑сервисами.
- BFF (gateway): `BFF_ISSUER`, `BFF_CLIENT_ID`, опционально `BFF_COOKIE_NAME` (по умолчанию `refresh_token`), `BFF_COOKIE_SAME_SITE`, `BFF_COOKIE_SECURE`, `BFF_COOKIE_DOMAIN`.
- Service‑to‑Service: `OIDC_ISSUER`, `S2S_CLIENT_ID`, `S2S_CLIENT_SECRET` — для сервисов, которые дергают другие по client‑credentials (см. модуль `service-auth-client`).
- Kafka/Redis: `KAFKA_BOOTSTRAP` (по умолчанию `kafka:9092`), `REDIS_HOST`/`REDIS_PORT` (по умолчанию `redis:6379`).
- Базы данных: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — для каждого сервиса свои (см. `src/main/resources/application.yml`).
- Внутренние вызовы: опциональный `INTERNAL_API_KEY` включается в некоторых сервисах для простых внутренних сценариев (замещается S2S OAuth).

Сборка образов:
- `docker compose build` (Dockerfile каждого сервиса сам собирает нужный модуль Maven).

Kubernetes:
- Применить неймспейс: `kubectl apply -f k8s/namespace.yaml`
- Для сервисов: отредактируй `image` и `DB_URL`/Kafka/Redis хосты под свой кластер и применяй `kubectl apply -f k8s/<service>/deployment.yaml`.

Аутентификация и токены (Keycloak):
- Браузерный вход через BFF: `http://localhost:8080/auth/login` — авторизация в Keycloak, `refresh_token` сохраняется в httpOnly cookie, шлюз автоматически подставляет `Authorization: Bearer ...` в запросы к микросервисам.
- Обновление AT: шлюз делает refresh по cookie автоматически; вручную — `POST /bff/refresh` (JSON `{access_token, expires_in}`) — полезно для SPA.
- CLI/dev: Direct Access Grants — `curl -s -X POST http://localhost:18080/realms/crp/protocol/openid-connect/token -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=crp-cli" -d "grant_type=password" -d "username=admin@crp.local" -d "password=admin" | jq -r .access_token`
- JWKS: `http://localhost:18080/realms/crp/protocol/openid-connect/certs`

Что дальше (усиление безопасности): см. `docs/security-hardening.md`. Включает:
- single‑flight refresh в gateway (Redis‑локи),
- ротацию refresh в Keycloak (revoke + max reuse = 0),
- требование `aud` во всех ресурс‑сервисах и переход на service‑to‑service JWT вместо `X-Internal-API-Key`.

Проверка сценария (curl):
1) Получи `AT=$(...)` как выше
2) Создай технику:
   - `curl -s http://localhost:8082/equipment -H "Authorization: Bearer $AT" -H "Content-Type: application/json" -d '{"type":"truck","model":"KamAZ","status":"AVAILABLE","price":1000000}'`
3) Создай заявку:
   - `curl -s http://localhost:8083/requests -H "Authorization: Bearer $AT" -H "Content-Type: application/json" -d '{"equipmentId":1,"requesterId":1001,"amount":1000000}'`
4) Утверди заявку (роль MANAGER у админа есть):
   - `curl -s -X PATCH http://localhost:8083/requests/1/approve -H "Authorization: Bearer $AT"`
   - Инвентарь зарезервирует технику через Kafka (см. логи), статус заявки станет `RESERVED`.
5) Отчет (Excel):
   - `curl -v -L http://localhost:8084/reports/equipment-by-status.xlsx -H "Authorization: Bearer $AT" -o equipment-by-status.xlsx`

План доработок (чтобы «воспроизвести достижения»):
- Auth: JWT (access/refresh), реализация RBAC, парольный хэш, публичные/защищенные эндпойнты
- Kafka: события procurement.requested/approved/rejected, inventory.reserved/released; идемпотентные консьюмеры, ретраи
- Redis: кэширование и/или хранение токенов/блокировок
- Liquibase: расширение схем, индексы, ограничение целостности
- Тесты: unit + интеграционные (Testcontainers для Postgres/Kafka)
- Reports: генерация файлов (Apache POI), хранение и раздача, асинхронные job’ы
- CI/CD: пайплайн (build → test → integration-test → docker build/push)

Примечание:
- Все сервисы переведены на проверку JWT по RS256 через OAuth2 Resource Server и Issuer Keycloak (JWKS). Для внутренних машинных вызовов временно поддерживается заголовок `X-Internal-API-Key`.
- BFF в `gateway-service`: маршруты `/auth/login`, `/auth/callback`, `/auth/logout`. Refresh хранится в `HttpOnly` cookie, access обновляется автоматически и добавляется в заголовок при проксировании.
- Kafka/Redis конфиги подключены, но бизнес-логика событий/кэшей не реализована — добавляй по мере разработки.

Вариант B (на будущее): BFF
- Для веб‑клиента добавьте слой BFF в `gateway-service`, храните refresh‑токен в httpOnly cookie и делайте авто‑refresh. В этом репо пока используется прямое получение токена для dev.

Навигация по исходникам (ничего править не нужно — всё готово)
- Маршрутизация API: `gateway-service/src/main/resources/application.yml:1`
- Заявки (оркестрация KYC/UW/ценообразования): `application-service/src/main/java/com/example/crp/app/web/ApplicationController.java:1`
- Калькулятор графика: `product-pricing-service/src/main/java/com/example/crp/pricing/web/PricingController.java:1`
- Договор: `agreement-service/src/main/java/com/example/crp/agreement/web/AgreementController.java:1`
- Инвойсы: `billing-service/src/main/java/com/example/crp/billing/web/BillingController.java:1`
- KYC/UW заглушки: `kyc-service/src/main/java/com/example/crp/kyc/web/KycController.java:1`, `underwriting-service/src/main/java/com/example/crp/underwriting/web/UnderwritingController.java:1`
- Клиенты: `customer-service/src/main/java/com/example/crp/customer/web/CustomerController.java:1`
- JWT/секреты: application.yml каждого сервиса (`SECURITY_JWT_SECRET`), внутренние вызовы (`INTERNAL_API_KEY`)

Сборка Maven (если хочешь проверить без Docker)
- Из корня репозитория: `mvn -q -DskipTests package`
- Отдельный модуль: `mvn -q -DskipTests -pl <module> -am package` (пример: `-pl procurement-service -am`)

Запуск через Docker (рекомендуется)
- `docker compose up -d --build` — собирает монорепозиторий и поднимает все сервисы и инфраструктуру.

Один скрипт — Docker
- `powershell -ExecutionPolicy Bypass -File scripts/up.ps1 -Rebuild` (или просто `scripts\up.ps1`)

Один скрипт — Kubernetes (minikube)
- Требуется: kubectl, minikube, docker, default StorageClass
- Запуск: `powershell -ExecutionPolicy Bypass -File scripts/k8s-up.ps1`
  - Поднимет minikube (если нужно), включит ingress, создаст namespace/secrets/инфру, БД (StatefulSet), соберёт и задеплоит все сервисы, применит ingress для gateway.
  - По итогу: `kubectl get pods -n crp` и открывайте http://crp.local (пропишите hosts на IP minikube, если нужно) или `kubectl port-forward deploy/gateway-service -n crp 8080:8080`.

Дополнительно (BPM + график + биллинг + платежи):
- BPM (Flowable): старт процесса заявки
  - `curl -s http://localhost:8095/bpm/process/start -H "X-Internal-API-Key: changeme" -H "Content-Type: application/json" -d '{"customerId":1,"amount":1000000,"termMonths":36,"rateAnnualPct":12}'`
- Сгенерировать график для договора:
  - `curl -s "http://localhost:8093/schedule/generate?agreementId=1&amount=1000000&termMonths=36&rateAnnualPct=12" -H "X-Internal-API-Key: changeme"`
- Авто-начисление инвойсов:
  - биллинг раз в минуту читает `/schedule/due?date=YYYY-MM-DD` и выпускает инвойсы, помечая элементы графика как INVOICED. Подождите до минуты или временно вызовите генерацию с ближайшей датой.
- Имитация платежа (вебхук):
  - `curl -s http://localhost:8092/payments/webhook -H "Content-Type: application/json" -d '{"eventId":"evt-1","invoiceId":1,"amount":12345.67}'`
  - сообщение попадёт в Kafka `payment.received`, биллинг отметит инвойс как PAID, бухгалтерия запишет проводки.
