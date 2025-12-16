package com.example.crp.inventory.web;

import com.example.crp.inventory.service.EquipmentDocumentService;
import com.example.crp.inventory.web.dto.InventoryDtos;
import com.example.crp.inventory.web.dto.InventoryMappers;
import jakarta.validation.constraints.NotNull;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/equipment")
public class EquipmentDocumentsController {

    private final EquipmentDocumentService service;

    public EquipmentDocumentsController(EquipmentDocumentService service) {
        this.service = service;
    }

    @GetMapping("/{equipmentId}/documents")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<InventoryDtos.DocumentDto> list(@PathVariable("equipmentId") Long equipmentId) {
        return service.list(equipmentId).stream().map(InventoryMappers::toDocument).toList();
    }

    @PostMapping(path = "/{equipmentId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> upload(@PathVariable("equipmentId") Long equipmentId,
                                    @RequestParam(value = "docType", required = false) String docType,
                                    @RequestPart("file") @NotNull MultipartFile file,
                                    Authentication auth,
                                    HttpServletRequest servletRequest) {
        try {
            String createdBy = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            var saved = service.upload(equipmentId, docType, file, createdBy, correlationId);
            return ResponseEntity.status(201).body(InventoryMappers.toDocument(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/documents/{id}/download")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<byte[]> download(@PathVariable("id") UUID id) {
        try {
            var meta = service.get(id);
            byte[] bytes = service.download(id);
            String contentType = meta.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : meta.getContentType();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + meta.getFileName())
                    .body(bytes);
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/documents/{id}/download-url")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<?> downloadUrl(@PathVariable("id") UUID id) {
        try {
            Optional<URI> url = service.downloadUrl(id);
            return url.<ResponseEntity<?>>map(u -> ResponseEntity.ok(java.util.Map.of("url", u.toString())))
                    .orElseGet(() -> ResponseEntity.status(409).body(java.util.Map.of("error", "presign_not_supported")));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
