# Backup & DR

## Postgres (docker compose)
- Create logical dumps for all service databases: `powershell -File scripts/ops/backup-postgres.ps1`.
- Restore a single database: `powershell -File scripts/ops/restore-postgres.ps1 -BackupFile backups/pg/<file>.sql -Container inventory-postgres -Database inventorydb -User inventory`.
- Store dumps off-host for DR and define RPO/RTO targets in ops documentation.

## MinIO (reports/procurement/inventory buckets)
- Create bucket backups: `powershell -File scripts/ops/backup-minio.ps1`.
- Restore with `mc mirror /backup/<bucket> local/<bucket>` inside a `minio/mc` container.

## Retention
- Outbox cleanup: `outbox.retention.days` and `outbox.retention.poll-ms` in each service.
- Reports cleanup: `reports.retention.days` and `reports.retention.poll-ms` in `reports-service`.
