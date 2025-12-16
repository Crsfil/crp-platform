package com.example.crp.procurement.web;

import com.example.crp.procurement.service.ProcurementAttachmentService;
import com.example.crp.procurement.web.dto.ProcurementDtos;
import com.example.crp.procurement.web.dto.ProcurementMappers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/attachments")
public class ProcurementAttachmentsController {
    private final ProcurementAttachmentService service;

    public ProcurementAttachmentsController(ProcurementAttachmentService service) {
        this.service = service;
    }

    @GetMapping("/{ownerType}/{ownerId}")
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN')")
    public List<ProcurementDtos.Attachment> list(@PathVariable String ownerType, @PathVariable Long ownerId) {
        return service.list(ownerType, ownerId).stream().map(ProcurementMappers::toAttachment).toList();
    }

    @PostMapping(path = "/{ownerType}/{ownerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<ProcurementDtos.Attachment> upload(@PathVariable String ownerType,
                                                             @PathVariable Long ownerId,
                                                             @RequestPart("file") MultipartFile file,
                                                             Authentication auth) {
        String createdBy = auth == null ? null : auth.getName();
        var saved = service.upload(ownerType, ownerId, file, createdBy);
        return ResponseEntity.status(201).body(ProcurementMappers.toAttachment(saved));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        try {
            var meta = service.get(id);
            byte[] bytes = service.download(id);
            return ResponseEntity.ok()
                    .contentType(meta.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(meta.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + meta.getFileName())
                    .body(bytes);
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/download-url")
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN')")
    public ResponseEntity<?> downloadUrl(@PathVariable UUID id) {
        try {
            Optional<URI> url = service.downloadUrl(id);
            return url.<ResponseEntity<?>>map(u -> ResponseEntity.ok(java.util.Map.of("url", u.toString())))
                    .orElseGet(() -> ResponseEntity.status(409).body(java.util.Map.of("error", "presign_not_supported")));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
