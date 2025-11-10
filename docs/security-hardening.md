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
- Использование: `refresh(sessionId, () -> callIdpRefresh())` возвращает `Mono<TokenPair>`.

## Ротация refresh‑токена/сессий в Keycloak
- Включено в экспорт Realm: `keycloak/realm-export/crp-realm.json`.
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

## mTLS (опционально, в дополнение к JWT)
- Рекомендуется для Intra‑cluster: включить взаимную аутентификацию на уровне канала.
- Kubernetes: cert‑manager → выдать pod‑certificates; в сервисах `server.ssl.*` + `server.ssl.client-auth=need`.
- Вызовы: настроить `WebClient`/`RestTemplate` на клиентский сертификат.

## Чек‑лист валидации
- Параллельные 401 → 1 запрос refresh на сессию, остальным прилетает результат.
- Keycloak после refresh выдаёт новый refresh; старый сразу недействителен.
- Ресурс‑сервисы отклоняют токены без корректной `aud`.
- Межсервисные вызовы ходят с service‑JWT; ключ заголовка больше не нужен.
