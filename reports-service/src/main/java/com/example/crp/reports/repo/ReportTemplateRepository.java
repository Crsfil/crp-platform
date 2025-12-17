package com.example.crp.reports.repo;

import com.example.crp.reports.domain.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {
    Optional<ReportTemplate> findByTemplateId(String templateId);
}
