# Аудит безопасности и аудитории (Nov 10, 2025)

## Сводка
- Gateway маршрутизирует 16 путей к внутренним сервисам и `/oidc/**` к Keycloak (см. `docs/diagrams/api-gateway-routes.mmd`).
- В ресурс‑сервисах включена проверка `aud` через модуль `common-security`; ожидаемая аудитория по умолчанию равна `spring.application.name` и везде `enforce-audience: true`.
- `issuer-uri` для resource server передаётся через env в `docker-compose.yml` для всех сервисов (Keycloak realm `crp`).
- Межсервисные вызовы настроены через `service-auth-client` (client credentials flow); для них в Keycloak экспортированы audience‑мапперы (см. `keycloak/realm-export/crp-realm.json`). Диаграмма: `docs/diagrams/service-interactions.mmd`.

## Обнаружения
- Gateway security (`gateway-service/src/main/java/.../SecurityConfig.java`): `permitAll` на все запросы. Авторизация выполняется на уровне ресурс‑сервисов; BFF‑фильтры (`BffAuthFilter`, `PreemptiveRefreshFilter`, `RetryOn401Filter`) лишь подставляют/обновляют токены, но не блокируют доступ сами по себе.
- Все ресурс‑сервисы подключают:
  - `spring-boot-starter-oauth2-resource-server`
  - `com.example.crp:common-security` → валидатор issuer + audience.
- Конфигурация:
  - `crp.security.jwt.enforce-audience: true` присутствует во всех `application.yml` ресурс‑сервисов.
  - `spring.security.oauth2.resourceserver.jwt.issuer-uri` задаётся через env для каждого сервиса и указывает на Keycloak realm `crp`.
- Keycloak realm export:
  - `crp-cli` (PKCE) с мапперами `aud-*` для всех ресурс‑сервисов.
  - Конфиденциальные клиенты `*-caller` с Service Accounts и точечными audience‑мапперами под реальные s2s вызовы.
  - Ресурс‑сервисы как bearer-only клиенты.
- Заголовок `X-Internal-API-Key`:
  - Фильтры присутствуют в некоторых сервисах, но включаются ТОЛЬКО при `security.internal-api-key.enabled=true`. В текущих `application.yml` этого флага нет, значит обход JWT по ключу отключён.

## Риски/замечания
- Gateway с `permitAll`: снаружи можно бить по любым путям, но ресурс‑сервисы вернут 401/403 при отсутствии валидного JWT. Рекомендуется ограничить анонимный доступ хотя бы до `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/auth/**`, `/oidc/**`.
- У многих сервисов в конфиге остались поля `security.jwt.secret` (наследие). Они не используются в режимах OAuth2 Resource Server — можно удалить для снижения конфигурационного шума.

## Рекомендации
1) Gateway: настроить авторизацию на уровне шлюза (опционально) — требовать JWT (resource server) или роль для внутренних путей, оставив permit на health/docs/auth/oidc.
2) Удалить неиспользуемые `security.jwt.secret` из `application.yml` ресурс‑сервисов.
3) Контроль дрейфа realm: при добавлении новых s2s связей добавлять audience‑мапперы в Keycloak, чтобы `aud` совпадал с целевым `spring.application.name`.
4) Метрики BFF refresh — следить за `gateway.refresh.*` и ошибками refresh (см. `docs/security-hardening.md`).

