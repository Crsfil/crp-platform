package com.example.crp.inventory.web;

import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.service.EquipmentDispositionDocumentsService;
import com.example.crp.inventory.service.EquipmentDispositionService;
import com.example.crp.inventory.web.dto.InventoryDtos;
import com.example.crp.inventory.web.dto.InventoryMappers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/equipment")
public class EquipmentDispositionController {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentDispositionService service;
    private final EquipmentDispositionDocumentsService documentsService;

    public EquipmentDispositionController(EquipmentRepository equipmentRepository,
                                          EquipmentDispositionService service,
                                          EquipmentDispositionDocumentsService documentsService) {
        this.equipmentRepository = equipmentRepository;
        this.service = service;
        this.documentsService = documentsService;
    }

    @GetMapping("/{id}/dispositions")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.DispositionDto>> list(@PathVariable("id") Long equipmentId) {
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.list(equipmentId).stream().map(InventoryMappers::toDisposition).toList());
    }

    @PostMapping("/{id}/dispositions")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> create(@PathVariable("id") Long equipmentId,
                                    @Valid @RequestBody InventoryDtos.CreateDisposition req,
                                    Authentication auth,
                                    HttpServletRequest servletRequest) {
        try {
            if (equipmentRepository.findById(equipmentId).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            var d = service.create(equipmentId, req.type(), req.plannedPrice(), req.currency(), req.counterpartyName(), req.counterpartyInn(),
                    req.locationId(), req.note(), user, correlationId);
            return ResponseEntity.status(201).body(InventoryMappers.toDisposition(d));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/dispositions/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(InventoryMappers.toDisposition(service.get(id)));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/dispositions/{id}/approve")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable("id") Long id,
                                     Authentication auth,
                                     HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toDisposition(service.approve(id, user, correlationId)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/dispositions/{id}/sale/contract")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> contractSale(@PathVariable("id") Long id,
                                          @Valid @RequestBody InventoryDtos.ContractSale req,
                                          Authentication auth,
                                          HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toDisposition(service.contractSale(
                    id, req.saleMethod(), req.lotNumber(), req.contractNumber(), user, correlationId
            )));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/dispositions/{id}/sale/invoice")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> invoiceSale(@PathVariable("id") Long id,
                                         @Valid @RequestBody InventoryDtos.InvoiceSale req,
                                         Authentication auth,
                                         HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toDisposition(service.invoiceSale(
                    id, req.invoiceNumber(), user, correlationId
            )));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/dispositions/{id}/sale/paid")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> markPaid(@PathVariable("id") Long id,
                                      @Valid @RequestBody InventoryDtos.MarkSalePaid req,
                                      Authentication auth,
                                      HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toDisposition(service.markPaid(
                    id, req.paidAt(), user, correlationId
            )));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/dispositions/{id}/complete")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> complete(@PathVariable("id") Long id,
                                      @Valid @RequestBody InventoryDtos.CompleteDisposition req,
                                      Authentication auth,
                                      HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toDisposition(service.complete(id, req.actualPrice(), req.performedAt(), user, correlationId, auth)));
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

    @PostMapping("/dispositions/{id}/cancel")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> cancel(@PathVariable("id") Long id,
                                    Authentication auth,
                                    HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(InventoryMappers.toDisposition(service.cancel(id, user, correlationId)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/dispositions/{id}/documents")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<?> documents(@PathVariable("id") Long id) {
        try {
            var d = service.get(id);
            return ResponseEntity.ok(documentsService.list(id).stream().map(InventoryMappers::toDispositionDocument).toList());
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(path = "/dispositions/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadDocument(@PathVariable("id") Long id,
                                            @RequestParam(value = "docType", required = false) String docType,
                                            @RequestParam(value = "relationType", required = false) String relationType,
                                            @RequestPart("file") MultipartFile file,
                                            Authentication auth,
                                            HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            var saved = documentsService.upload(id, docType, relationType, file, user, correlationId);
            return ResponseEntity.status(201).body(InventoryMappers.toDispositionDocument(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
