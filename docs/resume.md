# Senior Java‑разработчик

Senior Java‑разработчик с более чем 4,5 годами опыта в создании микросервисных решений для финтеха и корпоративных платформ. Обладает глубокой экспертизой в Java 17, Spring Boot, Hibernate, PostgreSQL, Kubernetes, автоматизации тестирования и CI/CD. Участвовал в разработке CRP‑платформы для лизингового бизнеса, реализовывал ключевые микросервисы, интеграции со сторонними API и модули автоматизации закупок.

## Опыт работы

### Агима
Java Developer • февраль 2020 – сентябрь 2022

Проект: Внутренняя CRM‑система для кредитных специалистов

Описание: Развитие и поддержка корпоративной CRM, улучшение бизнес‑логики, автоматизация тестирования и оптимизация работы с БД.

Стек: Java 11–17, Spring Boot, PostgreSQL, JUnit, Maven, Liquibase, Git, Docker

Обязанности:
- Разработка и поддержка REST‑сервисов
- Оптимизация SQL/HQL‑запросов и взаимодействия с БД
- Развитие инфраструктуры тестирования (JUnit, Mockito)
- Рефакторинг и документирование существующих модулей

Достижения:
- Перевёл легаси‑компоненты на Spring Boot, улучшив производительность API
- Повысил надёжность сборок за счёт внедрения unit‑ и integration‑тестов
- Участвовал в переходе системы на микросервисную архитектуру

### Газпромбанк Лизинг
Middle / Senior Java Developer • октябрь 2022  по н.в.

Проект: CRP‑система для управления лизингом техники и автоматизации закупок

Описание: Разработка микросервисной платформы для учёта техники, автоматизации бизнес‑процессов и формирования отчетности.

Стек: Java 17, Spring Boot, Spring Security, Hibernate, PostgreSQL, Kafka, Liquibase, Docker, Kubernetes, Prometheus, Grafana, Swagger, Bitbucket Pipelines

Обязанности:
- Проектирование и реализация микросервисов (Auth, Inventory, Procurement, Reports).
- Разработка и внедрение единого стека аутентификации на базе OIDC (Keycloak) и JWT (RS256) для всей платформы.
- Проектирование и внедрение RBAC‑модели (Role‑Based Access Control): роли/права, маппинг в токены (protocol mappers), применение правил в сервисах.
- Создание общих модулей безопасности (`common-security`) и клиента для межсервисного взаимодействия (`service-auth-client`).
- Реализация централизованного обновления токенов (BFF) с защитой от "шторма" обновлений.
- Создание сервиса формирования отчетов (Excel, Word) с использованием Apache POI.
- Проведение code review, проектирование архитектуры и миграция сервисов на новый стек безопасности.

Достижения:
- Разработал и вывел в продакшн 4 микросервиса (Auth/Inventory/Procurement/Reports) с интеграциями и отчётностью (Apache POI — генерация Excel/Word).
- Спроектировал и внедрил единый стек аутентификации: OIDC (OpenID Connect) на базе Keycloak (IdP), JWT RS256 (асимметричная подпись), строгая проверка `aud` (аудитория — целевой сервис).
- Реализовал RBAC: роли (ADMIN/MANAGER/ANALYST/USER), маппинг в claims (`roles`, `authorities`), правила доступа (@PreAuthorize, matchers). Документация: `docs/rbac.md`.
- Создал общий модуль безопасности `common-security` (автоконфигурация `JwtDecoder`, валидация `iss`/`aud`, plug-and-play для всех сервисов).
- Реализовал `service-auth-client` (`client-credentials` + фильтр `WebClient`) и перевёл s2s-вызовы на JWT; отказался от `X-Internal-API-Key`.
- Включил автообновление в BFF (Backend For Frontend) и защиту от «шторма» (single-flight через Redis), добавил метрики (успех/ошибка/тайминги).
- Убрал HMAC-секреты из сервисов, унифицировал конфиги (`issuer-uri`, `enforce-audience`), снизил конфигурационный долг и риски.

## Навыки
- Backend: Java 11–17, Spring Boot, Hibernate, JPA, Kafka, Redis
- Базы данных: PostgreSQL, Liquibase
- DevOps: Docker, Kubernetes, Bitbucket Pipelines, Prometheus, Grafana
- Тестирование: JUnit 5, Mockito, TestContainers
- Инструменты: Maven, Swagger/OpenAPI, Git, Jira, Confluence, Apache POI
