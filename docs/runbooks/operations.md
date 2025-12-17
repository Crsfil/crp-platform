# Operations Runbooks

## instance-down
- Verify container health: `docker compose ps` and service logs.
- Check DB/Kafka/Redis connectivity for the failed service.
- If a single container is stuck, restart the service container.
- If multiple services are down, verify Docker daemon and disk space.

## high-5xx-rate
- Identify top failing endpoints in Grafana (HTTP 5xx panel).
- Check recent deployments or schema migrations.
- Inspect service logs for stack traces and error spikes.

## error-budget-burn
- Confirm if errors are persistent or bursty (compare 5m vs 1h).
- Mitigate by scaling or rollback if tied to a recent change.
- Capture incident notes for postmortem.

## outbox-backlog
- Check Kafka availability and topic leaders.
- Inspect outbox tables for PENDING/SENT counts.
- If producer errors persist, restart the service after verifying Kafka.
- Requeue stuck events by resetting status if needed.

## outbox-stuck
- Check oldest outbox age via metrics (`outbox_oldest_seconds`) and logs for serialization errors.
- Verify DB connectivity and transaction locks on outbox tables.
- If the same payload fails repeatedly, route to invalid topic and mark key as invalid.

## sla-overdue
- Filter overdue service types in procurement dashboard.
- Check external vendor integration and notifications.
- Escalate to responsible team if SLA breach continues.

## dlq-invalid-events
- Inspect `<topic>.invalid` Kafka topics for payload/reason pairs.
- Check Redis set `kafka:invalid:keys` to prevent repeat processing of bad keys.
- Fix payload at source and replay with a new key; clear the old key if needed.

## idempotency-duplicates
- If clients see 409 with `idempotency_key_reused`, confirm they reused the same key.
- Verify gateway Redis availability and TTL (`gateway.idempotency.ttl`).
