package com.example.crp.reports.web;

import com.example.crp.reports.domain.ReportTemplate;
import com.example.crp.reports.service.ReportTemplateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/report-templates")
public class ReportTemplatesController {

    private final ReportTemplateService service;

    public ReportTemplatesController(ReportTemplateService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('REPORTS_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> upload(@RequestParam("templateId") String templateId,
                                    @RequestParam(value = "name", required = false) String name,
                                    @RequestParam(value = "version", required = false) String version,
                                    @RequestPart("file") MultipartFile file,
                                    org.springframework.security.core.Authentication auth) {
        try {
            String user = auth == null ? null : auth.getName();
            ReportTemplate saved = service.upload(templateId, name, version, file, user);
            return ResponseEntity.status(201).body(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public List<ReportTemplate> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.get(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        try {
            ReportTemplate t = service.get(id);
            byte[] bytes = service.download(id);
            String fileName = t.getFileName() != null ? t.getFileName() : ("template-" + id);
            String contentType = t.getContentType() != null ? t.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes);
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/download-url")
    @PreAuthorize("hasAuthority('REPORTS_READ') or hasRole('ADMIN')")
    public ResponseEntity<?> downloadUrl(@PathVariable Long id) {
        try {
            Optional<URI> url = service.downloadUrl(id);
            return url.<ResponseEntity<?>>map(u -> ResponseEntity.ok(java.util.Map.of("url", u.toString())))
                    .orElseGet(() -> ResponseEntity.status(409).body(java.util.Map.of("error", "presign_not_supported")));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
