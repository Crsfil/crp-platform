# Усиление безопасности: refresh, аудитория и внутренняя аутентификация

## Single‑flight refresh в gateway (Redis)
- Назначение: один запрос refresh на сессию при лавине параллельных запросов.
- Реализация:
  - `gateway-service`: Reactive Redis, класс `com.example.crp.gateway.refresh.SingleFlightRefreshManager`.
  - Ключи: `bff:refresh:lock:{sessionId}` (TTL ~7 c), `bff:refresh:result:{sessionId}` (TTL ~60 c).
  - Алгоритм: `SET NX PX` → refresh → публикация результата → Lua‑unlock.
- Конфигурация:
  - `gateway-service/src/main/resources/application.yml`: `spring.data.redis.host/port`.
  - `docker-compose.yml`: переменные `REDIS_HOST/REDIS_PORT` для `gateway-service`.
- Эндпоинт BFF: `POST /bff/refresh` — читает httpOnly cookie (по умолчанию `refresh_token`), делает refresh c координацией в Redis и:
  - возвращает JSON `{ "access_token": "...", "expires_in": 900 }`;
  - заменяет refresh‑cookie на новый (httpOnly, `secure`/`SameSite` из `bff.cookie.*`).
- Входные параметры:
  - cookie `refresh_token` (имя можно изменить `BFF_COOKIE_NAME`).

### Авто‑refresh на шлюзе при истекающем AT
- Фильтры gateway:
  - `com.example.crp.gateway.bff.BffAuthFilter` — прокидывание/ротация токенов на основе refresh‑cookie.
  - `com.example.crp.gateway.bff.PreemptiveRefreshFilter` — превентивный refresh при скором истечении AT; исключает `/bff/**`, `/auth/**`, `/oidc/**`, swagger/actuator.
  - Координация: `com.example.crp.gateway.refresh.SingleFlightRefreshManager`.
  - При успешном refresh обновляется refresh‑cookie и подменяется `Authorization` на новый AT.

## Ротация refresh‑токена/сессий в Keycloak
- Включено в экспорт Realm: `infrastructure/keycloak/realm-export/crp-realm.json`.
  - `revokeRefreshToken=true`, `refreshTokenMaxReuse=0`.
  - TTL по умолчанию: `accessTokenLifespan=900`, `refreshTokenLifespan=3600`.
- Клиенты:
  - `crp-cli` — публичный (PKCE) для BFF.
  - `inventory-service` — bearer‑only (ресурс).
  - `reports-service` — confidential + Service Accounts; audience‑mapper добавляет `inventory-service` в `aud`.
- Примечание: добавьте такие же пары клиентов и audience‑мапперов для каждого взаимодействия сервис→сервис.

## Требование `aud` в ресурс‑сервисах
- Автоконфигурация: модуль `common-security`.
  - Создаёт `JwtDecoder` c валидаторами `issuer` и `aud`.
  - Ожидаемая аудитория: `crp.security.jwt.audience` или `spring.application.name` (по умолчанию).
- Подключено во всех ресурс‑сервисах (`pom.xml` каждого сервиса содержит `common-security`).
- Конфигурация истца: `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` указывает на Keycloak Realm.

## Service‑to‑Service JWT вместо `X-Internal-API-Key`
- Клиентские токены: модуль `service-auth-client`.
  - `ClientCredentialsTokenManager` получает токен у IdP и кэширует его.
  - `BearerExchangeFilter` — фильтр для `WebClient`, автоматически добавляет `Authorization: Bearer <token>`.
- В Keycloak:
  - Для каждого вызывающего сервиса — confidential‑клиент с `Service Accounts Enabled`.
  - Добавить `Audience`‑маппер(ы) с целевым ресурсом (bearer‑only клиент цели).
- Переход:
  1) Подключить `service-auth-client` в вызывающих сервисах.
  2) Настроить `issuer`, `clientId/secret` через env.
  3) Удалить использование `X-Internal-API-Key` и заголовок из вызовов.
  4) По завершении — удалить `INTERNAL_API_KEY` из `docker-compose.yml`.

## RBAC (Role‑Based Access Control)
- Роли и права описаны в `docs/rbac.md` (realm roles, protocol mappers, применение в сервисах).
- В сервисах включена `@EnableMethodSecurity` и используются `JwtAuthenticationConverter` для маппинга `roles/authorities` в GrantedAuthorities.
- Рекомендация: критичные операции закрывать через `@PreAuthorize` и/или matchers на уровне `SecurityFilterChain`.

## mTLS (опционально, в дополнение к JWT)
- Рекомендуется для Intra‑cluster: включить взаимную аутентификацию на уровне канала.
- Kubernetes: cert‑manager → выдать pod‑certificates; в сервисах `server.ssl.*` + `server.ssl.client-auth=need`.
- Вызовы: настроить `WebClient`/`RestTemplate` на клиентский сертификат.

## Чек‑лист валидации
- Параллельные 401 → 1 запрос refresh на сессию, остальным прилетает результат.
- Keycloak после refresh выдаёт новый refresh; старый сразу недействителен.
- Ресурс‑сервисы отклоняют токены без корректной `aud`.
- Межсервисные вызовы ходят с service‑JWT; ключ заголовка больше не нужен.

## Метрики (Prometheus/Micrometer)
- `bff.refresh.singleflight` (tag `event`): `lock_acquired`, `lock_contended`, `refresh_success`, `refresh_error`.
- `bff.refresh.singleflight.timer`: время выполнения refresh инициатором (Timer).
- `gateway.refresh.preemptive` (tag `event`): `triggered`, `success`, `error` — проактивный refresh по `exp`.
- `gateway.refresh.retry401` (tag `event`): `triggered`, `success`, `error` — повтор после 401.
- `bff.refresh.endpoint` (tag `event`): `attempt`, `success`, `error` — ручной `/bff/refresh`.

## Добавление новых s2s связей (аудитория)
- Цель: чтобы целевой ресурс‑сервис принимал JWT с `aud` равным его `spring.application.name`.
- Шаги в Keycloak (realm `crp`):
  - Создать/использовать confidential‑клиент вызывающей стороны (`<caller>-caller`) с `Service Accounts Enabled` и секретом.
  - Убедиться, что целевой сервис существует как bearer‑only клиент (`<target-service>`).
  - В `Protocol Mappers` вызывающего клиента добавить `Audience`‑маппер:
    - `included.client.audience = <target-service>`
    - `access.token.claim = true`, `id.token.claim = false`.
- В вызывающем сервисе настроить `service-auth-client` (`issuer`, `clientId`, `clientSecret`).
- Проверка: запрос с полученным client‑credentials токеном к целевому сервису проходит; при удалении маппера целевой сервис отвечает 401/invalid_token (aud).
