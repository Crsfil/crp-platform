Infrastructure

Структура:
- `infrastructure/keycloak/` — экспорт realm (импортируется в Keycloak из docker-compose).
- `infrastructure/observability/` — конфиги Prometheus/Loki/Tempo/Grafana provisioning + Grafana Alloy.
- `infrastructure/k8s/` — Kubernetes-манифесты (namespace, infra, postgres, сервисы, observability).

Локально (Docker):
- `docker compose up -d --build` (без Camunda)
- `docker compose -f ../docker-compose.yml -f camunda/docker-compose.camunda.yml up -d --build` (с Camunda)
  - Grafana: `http://localhost:3000` (admin/admin)
  - Prometheus: `http://localhost:9090`
  - Loki: `http://localhost:3100`
  - Tempo: `http://localhost:3200` (OTLP: `4317/4318`)
  - Camunda Platform: `http://localhost:18081` (`/engine-rest`, Postgres: `camunda-postgres`)
  - BPMN/DMN модели: `process-models/` (деплой через `scripts/deploy-camunda-models.ps1`)

Kubernetes (minikube):
- `powershell -ExecutionPolicy Bypass -File scripts/k8s-up.ps1`
  - Накатит базовую инфру + `infrastructure/k8s/observability/` (Prometheus/Grafana/Loki/Tempo/Alloy).
