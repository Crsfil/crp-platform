package com.example.crp.reports.repo;

import com.example.crp.reports.domain.ReportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {
    long deleteByStatusInAndCreatedAtBefore(java.util.List<String> statuses, java.time.OffsetDateTime cutoff);
}
