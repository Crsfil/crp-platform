# Аудит безопасности и аудитории (обновлено Nov 12, 2025)

## Сводка
- auth-service: DEPRECATED; сервис выключен из docker-compose, основной IdP — Keycloak.
- Маршруты gateway вынесены в профиль по умолчанию (gateway-service/src/main/resources/application-default.yml); отдельный маршрут /oidc/** на Keycloak. Роут /auth/** больше не проксируется. Диаграмма: docs/diagrams/api-gateway-routes.mmd.
- В ресурс‑сервисах включена проверка `aud` через модуль `common-security`; ожидаемая аудитория по умолчанию равна `spring.application.name` и везде `enforce-audience: true`.
- `issuer-uri` для resource server передаётся через env в `docker-compose.yml` для всех сервисов (Keycloak realm `crp`).
- Межсервисные вызовы настроены через `service-auth-client` (client credentials flow); для них в Keycloak экспортированы audience‑мапперы (см. `keycloak/realm-export/crp-realm.json`). Диаграмма: `docs/diagrams/service-interactions.mmd`.

## Обнаружения
- Gateway security (`gateway-service/src/main/java/.../SecurityConfig.java`): `permitAll` на все запросы. BFF‑фильтр (`com.example.crp.gateway.bff.BffAuthFilter`) блокирует непубличные пути при отсутствии `Authorization` и refresh‑cookie; превентивный/повторный refresh реализованы фильтрами (`PreemptiveRefreshFilter`, `RetryOn401Filter`).
- Все ресурс‑сервисы подключают:
  - `spring-boot-starter-oauth2-resource-server`
  - `com.example.crp:common-security` → валидатор issuer + audience.
- Конфигурация:
  - `crp.security.jwt.enforce-audience: true` присутствует в `application.yml` ресурс‑сервисов.
  - `spring.security.oauth2.resourceserver.jwt.issuer-uri` задаётся через env для каждого сервиса и указывает на Keycloak realm `crp`.
- Keycloak realm export:
  - `crp-cli` (PKCE) с мапперами `aud-*` для всех ресурс‑сервисов.
  - Конфиденциальные клиенты `*-caller` с Service Accounts и точечными audience‑мапперами под реальные s2s вызовы.
  - Ресурс‑сервисы как bearer-only клиенты.
- Заголовок `X-Internal-API-Key`:
  - Фильтры присутствуют в некоторых сервисах, но включаются ТОЛЬКО при `security.internal-api-key.enabled=true`. В текущих `application.yml` этого флага нет, значит обход JWT по ключу отключён.

## Риски/замечания
- BFF фильтр ограничивает непубличные пути (без `Authorization`/refresh‑cookie) — публичными остаются: `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/bff/**` (где уместно), `/oidc/**`.
- Поля `security.jwt.secret` удалены из `application.yml` ресурс‑сервисов (наследие HMAC) — лишний шум устранён.

## Рекомендации
1) Gateway: при необходимости добавить ролевую проверку на уровне `SecurityWebFilterChain` (после подстановки AT BFF‑фильтром).
2) Очистить SECURITY_JWT_SECRET из docker-compose и k8s манифестов (переменные окружения, которые более не используются сервисами).
3) Контроль дрейфа realm: при добавлении новых s2s связей добавлять audience‑мапперы в Keycloak, чтобы `aud` совпадал с целевым `spring.application.name`.
4) Метрики BFF refresh — следить за `gateway.refresh.*` и ошибками refresh (см. `docs/security-hardening.md`).





