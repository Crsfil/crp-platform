package com.example.crp.reports.service;

import com.example.crp.reports.domain.ReportJob;
import com.example.crp.reports.repo.ReportJobRepository;
import jakarta.transaction.Transactional;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Component
public class HeavyReportJobExecutor implements Job {

    private final ReportJobRepository reportJobRepository;
    private final ReportGenerator reportGenerator;

    public HeavyReportJobExecutor(ReportJobRepository reportJobRepository,
                                  ReportGenerator reportGenerator) {
        this.reportJobRepository = reportJobRepository;
        this.reportGenerator = reportGenerator;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext context) {
        Long jobId = context.getMergedJobDataMap().getLong("jobId");
        ReportJob job = reportJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        if (!"PENDING".equals(job.getStatus())) {
            return;
        }
        job.setStatus("IN_PROGRESS");
        reportJobRepository.save(job);
        try {
            byte[] data;
            if ("EQUIPMENT_BY_STATUS_XLSX".equals(job.getType())) {
                data = reportGenerator.equipmentByStatusXlsx();
            } else if ("REQUESTS_BY_STATUS_XLSX".equals(job.getType())) {
                data = reportGenerator.requestsByStatusXlsx();
            } else if ("AGREEMENTS_PORTFOLIO_XLSX".equals(job.getType())) {
                data = reportGenerator.agreementsPortfolioXlsx();
            } else if ("INVOICES_AGING_XLSX".equals(job.getType())) {
                data = reportGenerator.invoicesAgingXlsx();
            } else if ("INVOICES_CASHFLOW_XLSX".equals(job.getType())) {
                data = reportGenerator.invoicesCashflowXlsx();
            } else if ("APPLICATIONS_KPI_XLSX".equals(job.getType())) {
                data = reportGenerator.applicationsKpiXlsx();
            } else {
                job.setStatus("FAILED");
                reportJobRepository.save(job);
                return;
            }
            Path dir = Path.of("reports");
            Files.createDirectories(dir);
            String fileName = "report-" + job.getId() + ".xlsx";
            Path path = dir.resolve(fileName);
            Files.write(path, data);
            job.setFilePath(path.toAbsolutePath().toString());
            job.setStatus("DONE");
            job.setCreatedAt(OffsetDateTime.now());
            reportJobRepository.save(job);
        } catch (Exception e) {
            job.setStatus("FAILED");
            reportJobRepository.save(job);
        }
    }
}
