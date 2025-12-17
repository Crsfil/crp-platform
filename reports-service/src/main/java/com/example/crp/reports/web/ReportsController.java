package com.example.crp.reports.web;

import com.example.crp.reports.service.ReportGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportsController {
    private final ReportGenerator generator;
    public ReportsController(ReportGenerator generator) { this.generator = generator; }

    @GetMapping("/equipment-by-status.xlsx")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> equipmentByStatus() {
        byte[] data = generator.equipmentByStatusXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=equipment-by-status.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/requests.xlsx")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> requests() {
        byte[] data = generator.requestsByStatusXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=requests.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/procurement-pipeline.xlsx")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> procurementPipeline() {
        byte[] data = generator.procurementPipelineXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=procurement-pipeline.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/supplier-spend.xlsx")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> supplierSpend() {
        byte[] data = generator.supplierSpendXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=supplier-spend.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/repossessed-portfolio.xlsx")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> repossessedPortfolio() {
        byte[] data = generator.repossessedPortfolioXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repossessed-portfolio.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/storage-costs.xlsx")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> storageCosts() {
        byte[] data = generator.storageCostsXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=storage-costs.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/disposition-results.xlsx")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> dispositionResults() {
        byte[] data = generator.dispositionResultsXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=disposition-results.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
