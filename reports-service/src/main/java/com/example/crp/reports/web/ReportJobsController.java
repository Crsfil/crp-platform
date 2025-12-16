package com.example.crp.reports.web;

import com.example.crp.reports.domain.ReportJob;
import com.example.crp.reports.repo.ReportJobRepository;
import com.example.crp.reports.service.ReportJobScheduler;
import com.example.crp.reports.storage.ReportStorage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/report-jobs")
public class ReportJobsController {

    private final ReportJobScheduler scheduler;
    private final ReportJobRepository repository;
    private final ReportStorage reportStorage;

    public ReportJobsController(ReportJobScheduler scheduler,
                                ReportJobRepository repository,
                                ReportStorage reportStorage) {
        this.scheduler = scheduler;
        this.repository = repository;
        this.reportStorage = reportStorage;
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

    @PostMapping("/procurement-pipeline")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ReportJob createProcurementPipelineJob() {
        return scheduler.scheduleProcurementPipelineReport();
    }

    @PostMapping("/supplier-spend")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ReportJob createSupplierSpendJob() {
        return scheduler.scheduleSupplierSpendReport();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<ReportJob> getJob(@PathVariable Long id) {
        Optional<ReportJob> job = repository.findById(id);
        return job.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ReportJob job = repository.findById(id).orElse(null);
        if (job == null || !"DONE".equals(job.getStatus())) {
            return ResponseEntity.notFound().build();
        }
        ReportStorage.StoredReportRef ref = toRef(job);
        if (ref == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = reportStorage.get(ref);
        String fileName = job.getFileName() != null ? job.getFileName() : ("report-" + id + ".xlsx");
        String contentType = job.getContentType() != null ? job.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    @GetMapping("/{id}/download-url")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<?> downloadUrl(@PathVariable Long id) {
        ReportJob job = repository.findById(id).orElse(null);
        if (job == null || !"DONE".equals(job.getStatus())) {
            return ResponseEntity.notFound().build();
        }
        ReportStorage.StoredReportRef ref = toRef(job);
        if (ref == null) {
            return ResponseEntity.notFound().build();
        }
        Optional<URI> url = reportStorage.presignGet(ref);
        return url.<ResponseEntity<?>>map(u -> ResponseEntity.ok(java.util.Map.of("url", u.toString())))
                .orElseGet(() -> ResponseEntity.status(409).body(java.util.Map.of("error", "presign_not_supported")));
    }

    private static ReportStorage.StoredReportRef toRef(ReportJob job) {
        if (job.getStorageType() != null && job.getStorageLocation() != null) {
            return new ReportStorage.StoredReportRef(job.getStorageType(), job.getStorageLocation());
        }
        if (job.getFilePath() != null) {
            return new ReportStorage.StoredReportRef("FILESYSTEM", job.getFilePath());
        }
        return null;
    }
}
