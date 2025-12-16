package com.example.crp.inventory.web;

import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.service.EquipmentInspectionService;
import com.example.crp.inventory.web.dto.InventoryDtos;
import com.example.crp.inventory.web.dto.InventoryMappers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/equipment")
public class EquipmentInspectionController {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentInspectionService service;

    public EquipmentInspectionController(EquipmentRepository equipmentRepository, EquipmentInspectionService service) {
        this.equipmentRepository = equipmentRepository;
        this.service = service;
    }

    @GetMapping("/{id}/inspections")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.InspectionDto>> list(@PathVariable("id") Long equipmentId) {
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.list(equipmentId).stream().map(InventoryMappers::toInspection).toList());
    }

    @PostMapping("/{id}/inspections")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> create(@PathVariable("id") Long equipmentId,
                                    @Valid @RequestBody InventoryDtos.CreateInspection req,
                                    Authentication auth,
                                    HttpServletRequest servletRequest) {
        try {
            if (equipmentRepository.findById(equipmentId).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            var saved = service.create(equipmentId, req.type(), req.locationId(), req.summary(), user, correlationId, auth);
            return ResponseEntity.status(201).body(InventoryMappers.toInspection(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/inspections/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(InventoryMappers.toInspection(service.get(id)));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/inspections/{id}/findings")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.InspectionFindingDto>> findings(@PathVariable("id") Long id) {
        try {
            service.get(id); // verify exists
            return ResponseEntity.ok(service.findings(id).stream().map(InventoryMappers::toInspectionFinding).toList());
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/inspections/{id}/findings")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> addFinding(@PathVariable("id") Long id,
                                        @Valid @RequestBody InventoryDtos.AddInspectionFinding req) {
        try {
            var saved = service.addFinding(id, req.code(), req.severity(), req.description(), req.estimatedCost());
            return ResponseEntity.status(201).body(InventoryMappers.toInspectionFinding(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/inspections/{id}/submit")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> submit(@PathVariable("id") Long id,
                                    Authentication auth,
                                    HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toInspection(service.submit(id, user, correlationId)));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/inspections/{id}/approve")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable("id") Long id,
                                     Authentication auth,
                                     HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toInspection(service.approve(id, user, correlationId)));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/inspections/{id}/complete")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> complete(@PathVariable("id") Long id,
                                      @Valid @RequestBody InventoryDtos.CompleteInspection req,
                                      Authentication auth,
                                      HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toInspection(service.complete(
                    id, req.conclusion(), req.recommendedAction(), req.estimatedRepairCost(), user, correlationId, auth
            )));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/inspections/{id}/cancel")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> cancel(@PathVariable("id") Long id,
                                    Authentication auth,
                                    HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toInspection(service.cancel(id, user, correlationId)));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/inspections/{id}/documents")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.InspectionDocumentLinkDto>> documents(@PathVariable("id") Long id) {
        try {
            service.get(id); // verify exists
            return ResponseEntity.ok(service.documents(id).stream().map(InventoryMappers::toInspectionDocument).toList());
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(path = "/inspections/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadDocument(@PathVariable("id") Long id,
                                            @RequestParam(value = "docType", required = false) String docType,
                                            @RequestParam(value = "relationType", required = false) String relationType,
                                            @RequestPart("file") @NotNull MultipartFile file,
                                            Authentication auth,
                                            HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            var saved = service.uploadDocument(id, docType, relationType, file, user, correlationId);
            return ResponseEntity.status(201).body(InventoryMappers.toInspectionDocument(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}

