package com.example.crp.inventory.web;

import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.service.EquipmentRepairService;
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
public class EquipmentRepairController {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentRepairService service;

    public EquipmentRepairController(EquipmentRepository equipmentRepository, EquipmentRepairService service) {
        this.equipmentRepository = equipmentRepository;
        this.service = service;
    }

    @GetMapping("/{id}/repairs")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.RepairOrderDto>> list(@PathVariable("id") Long equipmentId) {
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.list(equipmentId).stream().map(InventoryMappers::toRepair).toList());
    }

    @PostMapping("/{id}/repairs")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> create(@PathVariable("id") Long equipmentId,
                                    @Valid @RequestBody InventoryDtos.CreateRepairOrder req,
                                    Authentication auth,
                                    HttpServletRequest servletRequest) {
        try {
            if (equipmentRepository.findById(equipmentId).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            var saved = service.create(equipmentId, req.inspectionId(), req.repairLocationId(), req.vendorName(), req.vendorInn(),
                    req.plannedCost(), req.currency(), req.note(), user, correlationId, auth);
            return ResponseEntity.status(201).body(InventoryMappers.toRepair(saved));
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

    @GetMapping("/repairs/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(InventoryMappers.toRepair(service.get(id)));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/repairs/{id}/approve")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable("id") Long id,
                                     Authentication auth,
                                     HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toRepair(service.approve(id, user, correlationId)));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/repairs/{id}/start")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> start(@PathVariable("id") Long id,
                                   @Valid @RequestBody InventoryDtos.StartRepair req,
                                   Authentication auth,
                                   HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toRepair(service.start(id, req.startedAt(), user, correlationId, auth)));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/repairs/{id}/complete")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> complete(@PathVariable("id") Long id,
                                      @Valid @RequestBody InventoryDtos.CompleteRepair req,
                                      Authentication auth,
                                      HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            boolean markAvailable = req.markAvailable() != null && req.markAvailable();
            boolean createInspection = req.createPostRepairInspection() != null && req.createPostRepairInspection();
            return ResponseEntity.ok(InventoryMappers.toRepair(service.complete(
                    id, req.actualCost(), req.completedAt(), markAvailable, createInspection, req.note(), user, correlationId, auth
            )));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/repairs/{id}/lines")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.RepairLineDto>> lines(@PathVariable("id") Long id) {
        try {
            service.get(id); // verify exists
            return ResponseEntity.ok(service.lines(id).stream().map(InventoryMappers::toRepairLine).toList());
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/repairs/{id}/lines")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> addLine(@PathVariable("id") Long id,
                                     @Valid @RequestBody InventoryDtos.AddRepairLine req) {
        try {
            var saved = service.addLine(id, req.kind(), req.description(), req.quantity(), req.uom(), req.unitCost());
            return ResponseEntity.status(201).body(InventoryMappers.toRepairLine(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/repairs/{id}/cancel")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> cancel(@PathVariable("id") Long id,
                                    @RequestParam(value = "note", required = false) String note,
                                    Authentication auth,
                                    HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toRepair(service.cancel(id, note, user, correlationId)));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/repairs/{id}/documents")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.RepairDocumentLinkDto>> documents(@PathVariable("id") Long id) {
        try {
            service.get(id); // verify exists
            return ResponseEntity.ok(service.documents(id).stream().map(InventoryMappers::toRepairDocument).toList());
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(path = "/repairs/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
            return ResponseEntity.status(201).body(InventoryMappers.toRepairDocument(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
