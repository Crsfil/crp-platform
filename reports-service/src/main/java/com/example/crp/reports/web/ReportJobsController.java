package com.example.crp.reports.web;

import com.example.crp.reports.domain.ReportJob;
import com.example.crp.reports.repo.ReportJobRepository;
import com.example.crp.reports.service.ReportJobScheduler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RestController
@RequestMapping("/report-jobs")
public class ReportJobsController {

    private final ReportJobScheduler scheduler;
    private final ReportJobRepository repository;

    public ReportJobsController(ReportJobScheduler scheduler,
                                ReportJobRepository repository) {
        this.scheduler = scheduler;
        this.repository = repository;
    }

    @PostMapping("/equipment-by-status")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ReportJob createEquipmentJob() {
        return scheduler.scheduleEquipmentReport();
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ReportJob createRequestsJob() {
        return scheduler.scheduleRequestsReport();
    }

    @PostMapping("/agreements-portfolio")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ReportJob createAgreementsPortfolioJob() {
        return scheduler.scheduleAgreementsPortfolioReport();
    }

    @PostMapping("/invoices-aging")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ReportJob createInvoicesAgingJob() {
        return scheduler.scheduleInvoicesAgingReport();
    }

    @PostMapping("/invoices-cashflow")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ReportJob createInvoicesCashflowJob() {
        return scheduler.scheduleInvoicesCashflowReport();
    }

    @PostMapping("/applications-kpi")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ReportJob createApplicationsKpiJob() {
        return scheduler.scheduleApplicationsKpiReport();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<ReportJob> getJob(@PathVariable Long id) {
        Optional<ReportJob> job = repository.findById(id);
        return job.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) throws IOException {
        ReportJob job = repository.findById(id).orElse(null);
        if (job == null || job.getFilePath() == null || !"DONE".equals(job.getStatus())) {
            return ResponseEntity.notFound().build();
        }
        Path path = Path.of(job.getFilePath());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
