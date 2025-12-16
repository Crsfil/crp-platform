package com.example.crp.reports.service;

import com.example.crp.reports.domain.ReportJob;
import com.example.crp.reports.repo.ReportJobRepository;
import org.quartz.*;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

@Service
public class ReportJobScheduler {

    private final Scheduler scheduler;
    private final ReportJobRepository reportJobRepository;

    public ReportJobScheduler(SchedulerFactoryBean factory,
                              ReportJobRepository reportJobRepository) throws SchedulerException {
        this.scheduler = factory.getScheduler();
        this.reportJobRepository = reportJobRepository;
    }

    public ReportJob scheduleEquipmentReport() {
        ReportJob job = new ReportJob();
        job.setType("EQUIPMENT_BY_STATUS_XLSX");
        job.setStatus("PENDING");
        reportJobRepository.save(job);
        scheduleQuartzJob(job.getId());
        return job;
    }

    public ReportJob scheduleRequestsReport() {
        ReportJob job = new ReportJob();
        job.setType("REQUESTS_BY_STATUS_XLSX");
        job.setStatus("PENDING");
        reportJobRepository.save(job);
        scheduleQuartzJob(job.getId());
        return job;
    }

    public ReportJob scheduleAgreementsPortfolioReport() {
        ReportJob job = new ReportJob();
        job.setType("AGREEMENTS_PORTFOLIO_XLSX");
        job.setStatus("PENDING");
        reportJobRepository.save(job);
        scheduleQuartzJob(job.getId());
        return job;
    }

    public ReportJob scheduleInvoicesAgingReport() {
        ReportJob job = new ReportJob();
        job.setType("INVOICES_AGING_XLSX");
        job.setStatus("PENDING");
        reportJobRepository.save(job);
        scheduleQuartzJob(job.getId());
        return job;
    }

    public ReportJob scheduleInvoicesCashflowReport() {
        ReportJob job = new ReportJob();
        job.setType("INVOICES_CASHFLOW_XLSX");
        job.setStatus("PENDING");
        reportJobRepository.save(job);
        scheduleQuartzJob(job.getId());
        return job;
    }

    public ReportJob scheduleApplicationsKpiReport() {
        ReportJob job = new ReportJob();
        job.setType("APPLICATIONS_KPI_XLSX");
        job.setStatus("PENDING");
        reportJobRepository.save(job);
        scheduleQuartzJob(job.getId());
        return job;
    }

    public ReportJob scheduleProcurementPipelineReport() {
        ReportJob job = new ReportJob();
        job.setType("PROCUREMENT_PIPELINE_XLSX");
        job.setStatus("PENDING");
        reportJobRepository.save(job);
        scheduleQuartzJob(job.getId());
        return job;
    }

    public ReportJob scheduleSupplierSpendReport() {
        ReportJob job = new ReportJob();
        job.setType("SUPPLIER_SPEND_XLSX");
        job.setStatus("PENDING");
        reportJobRepository.save(job);
        scheduleQuartzJob(job.getId());
        return job;
    }

    private void scheduleQuartzJob(Long jobId) {
        try {
            JobDetail detail = JobBuilder.newJob(HeavyReportJobExecutor.class)
                    .withIdentity("reportJob-" + jobId, "reports")
                    .usingJobData("jobId", jobId)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("reportTrigger-" + jobId, "reports")
                    .startNow()
                    .build();

            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to schedule report job " + jobId, e);
        }
    }
}
