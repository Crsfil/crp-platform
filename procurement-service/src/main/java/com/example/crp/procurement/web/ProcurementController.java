package com.example.crp.procurement.web;

import com.example.crp.procurement.domain.ProcurementRequest;
import com.example.crp.procurement.domain.ProcurementRequestLine;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import com.example.crp.procurement.service.ProcurementAttachmentService;
import com.example.crp.procurement.service.ProcurementRequestService;
import com.example.crp.procurement.service.PurchaseOrderService;
import com.example.crp.procurement.web.dto.ProcurementDtos;
import com.example.crp.procurement.web.dto.ProcurementMappers;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/requests")
public class ProcurementController {
    private final ProcurementRequestRepository repository;
    private final ProcurementRequestService requestService;
    private final PurchaseOrderService purchaseOrderService;
    private final ProcurementAttachmentService attachmentService;

    public ProcurementController(ProcurementRequestRepository repository,
                                 ProcurementRequestService requestService,
                                 PurchaseOrderService purchaseOrderService,
                                 ProcurementAttachmentService attachmentService) {
        this.repository = repository;
        this.requestService = requestService;
        this.purchaseOrderService = purchaseOrderService;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<ProcurementDtos.RequestSummary> list() {
        return requestService.list().stream().map(ProcurementMappers::toSummary).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ProcurementDtos.RequestDetails create(@Valid @RequestBody ProcurementDtos.CreateRequest req, Authentication auth) {
        ProcurementRequest pr = new ProcurementRequest();
        pr.setKind(req.kind());
        pr.setTitle(req.title());
        pr.setRequestNumber(req.requestNumber());
        pr.setEquipmentId(req.equipmentId());
        pr.setRequesterId(req.requesterId());
        pr.setCurrency(req.currency());
        pr.setCostCenter(req.costCenter());
        pr.setNeedByDate(req.needByDate());
        if (req.lines() != null) {
            pr.setLines(req.lines().stream().map(l -> {
                ProcurementRequestLine line = new ProcurementRequestLine();
                line.setDescription(l.description());
                line.setQuantity(l.quantity());
                line.setUom(l.uom());
                line.setUnitPrice(l.unitPrice());
                line.setNeedByDate(l.needByDate());
                return line;
            }).toList());
        }
        ProcurementRequest saved = requestService.create(pr);
        return ProcurementMappers.toDetails(saved, attachmentService.list(ProcurementAttachmentService.OWNER_REQUEST, saved.getId()));
    }

    @PostMapping("/legacy")
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ProcurementDtos.RequestDetails createLegacy(@Valid @RequestBody ProcurementRequest pr) {
        ProcurementRequest saved = requestService.create(pr);
        return ProcurementMappers.toDetails(saved, attachmentService.list(ProcurementAttachmentService.OWNER_REQUEST, saved.getId()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PROCUREMENT_APPROVE') or hasRole('ADMIN')")
    public ResponseEntity<ProcurementDtos.RequestDetails> approve(@PathVariable Long id, Authentication auth) {
        try {
            ProcurementRequest pr = requestService.approve(id, auth == null ? null : auth.getName());
            return ResponseEntity.ok(ProcurementMappers.toDetails(pr, attachmentService.list(ProcurementAttachmentService.OWNER_REQUEST, pr.getId())));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PROCUREMENT_APPROVE') or hasRole('ADMIN')")
    public ResponseEntity<ProcurementDtos.RequestDetails> reject(@PathVariable Long id, Authentication auth) {
        try {
            ProcurementRequest pr = requestService.reject(id, auth == null ? null : auth.getName());
            return ResponseEntity.ok(ProcurementMappers.toDetails(pr, attachmentService.list(ProcurementAttachmentService.OWNER_REQUEST, pr.getId())));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<ProcurementDtos.RequestDetails> get(@PathVariable Long id) {
        return repository.findWithLinesById(id)
                .map(pr -> ResponseEntity.ok(ProcurementMappers.toDetails(pr, attachmentService.list(ProcurementAttachmentService.OWNER_REQUEST, pr.getId()))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/purchase-orders")
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> createPurchaseOrder(@PathVariable Long id, @RequestParam(required = false) Long supplierId) {
        try {
            var po = purchaseOrderService.createFromRequest(id, supplierId);
            return ResponseEntity.status(201).body(java.util.Map.of("purchaseOrderId", po.getId()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        }
    }
}
