package com.example.crp.reports.service;

import com.example.crp.reports.domain.ReportJob;
import com.example.crp.reports.repo.ReportJobRepository;
import com.example.crp.reports.storage.ReportStorage;
import jakarta.transaction.Transactional;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class HeavyReportJobExecutor implements Job {

    private static final Logger log = LoggerFactory.getLogger(HeavyReportJobExecutor.class);
    private static final String XLSX_CT = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportJobRepository reportJobRepository;
    private final ReportGenerator reportGenerator;
    private final ReportStorage reportStorage;
    private final com.example.crp.reports.messaging.ReportsEventPublisher eventPublisher;

    public HeavyReportJobExecutor(ReportJobRepository reportJobRepository,
                                  ReportGenerator reportGenerator,
                                  ReportStorage reportStorage,
                                  com.example.crp.reports.messaging.ReportsEventPublisher eventPublisher) {
        this.reportJobRepository = reportJobRepository;
        this.reportGenerator = reportGenerator;
        this.reportStorage = reportStorage;
        this.eventPublisher = eventPublisher;
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
        job.setStartedAt(OffsetDateTime.now());
        job.setFinishedAt(null);
        job.setErrorMessage(null);
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
            } else if ("PROCUREMENT_PIPELINE_XLSX".equals(job.getType())) {
                data = reportGenerator.procurementPipelineXlsx();
            } else if ("SUPPLIER_SPEND_XLSX".equals(job.getType())) {
                data = reportGenerator.supplierSpendXlsx();
            } else if ("REPOSSESSED_PORTFOLIO_XLSX".equals(job.getType())) {
                data = reportGenerator.repossessedPortfolioXlsx();
            } else if ("STORAGE_COSTS_XLSX".equals(job.getType())) {
                data = reportGenerator.storageCostsXlsx();
            } else if ("DISPOSITION_RESULTS_XLSX".equals(job.getType())) {
                data = reportGenerator.dispositionResultsXlsx();
            } else {
                job.setStatus("FAILED");
                job.setFinishedAt(OffsetDateTime.now());
                job.setErrorMessage("Unknown report type: " + job.getType());
                reportJobRepository.save(job);
                return;
            }

            String fileName = "report-" + job.getId() + ".xlsx";
            ReportStorage.StoredReport stored = reportStorage.put("job-" + job.getId(), fileName, XLSX_CT, data);

            job.setFileName(stored.fileName());
            job.setContentType(stored.contentType());
            job.setFileSize(stored.sizeBytes());
            job.setSha256(stored.sha256Hex());
            job.setStorageType(stored.storageType());
            job.setStorageLocation(stored.location());
            job.setFilePath("FILESYSTEM".equalsIgnoreCase(stored.storageType()) ? stored.location() : null);
            job.setStatus("DONE");
            job.setFinishedAt(OffsetDateTime.now());
            reportJobRepository.save(job);
            eventPublisher.publishDone(job);
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setFinishedAt(OffsetDateTime.now());
            job.setErrorMessage(trim(e.getMessage(), 1000));
            log.warn("Report job {} failed", jobId, e);
            reportJobRepository.save(job);
            eventPublisher.publishFailed(job);
        }
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}
