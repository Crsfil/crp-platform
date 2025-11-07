package com.example.crp.reports.repo;

import com.example.crp.reports.domain.ReportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {
}

