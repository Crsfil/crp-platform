Infrastructure

Структура:
- `infrastructure/keycloak/` — экспорт realm (импортируется в Keycloak из docker-compose).
- `infrastructure/observability/` — конфиги Prometheus/Loki/Tempo/Grafana provisioning + Grafana Alloy.
- `infrastructure/k8s/` — Kubernetes-манифесты (namespace, infra, postgres, сервисы, observability).

Локально (Docker):
- `docker compose up -d --build`
  - Grafana: `http://localhost:3000` (admin/admin)
  - Prometheus: `http://localhost:9090`
  - Loki: `http://localhost:3100`
  - Tempo: `http://localhost:3200` (OTLP: `4317/4318`)

Kubernetes (minikube):
- `powershell -ExecutionPolicy Bypass -File scripts/k8s-up.ps1`
  - Накатит базовую инфру + `infrastructure/k8s/observability/` (Prometheus/Grafana/Loki/Tempo/Alloy).
