# RBAC в CRP (роль‑based access control)

Цель: описать роли, маппинг прав в токены (claims) и способы применения правил в микросервисах.

## Роли (realm roles в Keycloak)
- ADMIN — полный доступ к административным операциям.
- MANAGER — операции управления объектами домена (утверждения/публикации/закрытия).
- ANALYST — чтение отчётов/аналитики; ограниченные изменения.
- USER — базовые пользовательские операции.

Роли заданы в экспорте realm `infrastructure/keycloak/realm-export/crp-realm.json` (realm.roles).

## Маппинг ролей в токены (claims)
- Protocol mappers в клиенте `crp-cli` добавляют:
  - claim `roles` (массив) — роли realm без префиксов.
  - claim `authorities` (массив) — роли realm, удобные для прямого маппинга в Spring.
- Для сервисных клиентов (client_credentials) роли задаются на стороне клиента; при необходимости используйте client roles.

## Применение в сервисах (Spring Security)
- Все сервисы работают как Resource Server (OAuth2), валидируют `issuer` и `audience` (через модуль `common-security`).
- Роли из токена конвертируются в GrantedAuthorities через `JwtAuthenticationConverter`.
  - Принята схема: `roles` → `ROLE_*`, `authorities` → без префикса.
  - Примеры смотрите в `*/src/main/java/.../config/SecurityConfig.java` (kyc-service, schedule-service, payments-service и др.).
- Способы навешивания правил:
  - На маршруты: `.authorizeHttpRequests(a -> a.requestMatchers("/admin/**").hasRole("ADMIN").anyRequest().authenticated())`.
  - На методы: `@PreAuthorize("hasRole('MANAGER') or hasAuthority('REPORT_READ')")` (включено `@EnableMethodSecurity`).

## Аудитория (aud)
- Каждый ресурс‑сервис принимает токен только со своей аудиторией (`aud` = `spring.application.name`).
- Включено флагом `crp.security.jwt.enforce-audience: true` и реализовано в модуле `common-security`.

## Межсервисные вызовы (S2S, client_credentials)
- Используем `service-auth-client`:
  - `ClientCredentialsTokenManager` — получает/кэширует access_token у Keycloak.
  - `BearerExchangeFilter` — добавляет `Authorization: Bearer <token>` в `WebClient`.
- В Keycloak для вызывающих сервисов создаются конфиденциальные клиенты `<caller>-caller` (Service Accounts Enabled), для целевых — bearer‑only клиенты `<target>`.
- Для каждого направления добавляется Audience‑mapper (`included.client.audience = <target>`).

## SSO и MFA (статус/план)
- SSO (Single Sign‑On): фактически есть для всех приложений в пределах одного realm Keycloak и OIDC‑клиентов. Новые приложения подключаются через OIDC‑клиент в том же realm.
- SAML: не настроен. Можно добавить в Keycloak SAML‑клиент для внешних систем.
- MFA (многофакторная аутентификация): пока не включена. Рекомендуемая опция — TOTP (Time‑based OTP) в Keycloak; для SMS‑OTP нужен кастом‑провайдер/SPI или внешний сервис.

## Рекомендации эксплуатации
- TTL: access_token 10–15 мин, refresh_token 30–60 мин (или бизнес‑политика). BFF реализует single‑flight refresh.
- Масштаб Keycloak: 2–3 реплики + внешняя БД Postgres с резервным копированием; включить метрики и мониторинг.
- Тестирование: интеграционные кейсы 401 (нет/неверный aud/issuer), 403 (недостаточно ролей), happy‑path для ролей.

