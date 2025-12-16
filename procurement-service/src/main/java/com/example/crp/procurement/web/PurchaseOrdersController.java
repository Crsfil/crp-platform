package com.example.crp.procurement.web;

import com.example.crp.procurement.repo.PurchaseOrderRepository;
import com.example.crp.procurement.service.ProcurementAttachmentService;
import com.example.crp.procurement.service.PurchaseOrderService;
import com.example.crp.procurement.web.dto.ProcurementDtos;
import com.example.crp.procurement.web.dto.ProcurementMappers;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchase-orders")
public class PurchaseOrdersController {
    private final PurchaseOrderRepository repository;
    private final PurchaseOrderService service;
    private final ProcurementAttachmentService attachmentService;

    public PurchaseOrdersController(PurchaseOrderRepository repository,
                                    PurchaseOrderService service,
                                    ProcurementAttachmentService attachmentService) {
        this.repository = repository;
        this.service = service;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<ProcurementDtos.PurchaseOrderSummary> list() {
        return repository.findAll().stream()
                .map(ProcurementMappers::toPurchaseOrderSummary)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<ProcurementDtos.PurchaseOrderDto> get(@PathVariable Long id) {
        return repository.findWithLinesById(id)
                .map(po -> ResponseEntity.ok(ProcurementMappers.toPurchaseOrder(po, attachmentService.list(ProcurementAttachmentService.OWNER_PURCHASE_ORDER, po.getId()))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody ProcurementDtos.CreatePurchaseOrder req) {
        try {
            var po = service.createFromRequest(req.requestId(), req.supplierId());
            return ResponseEntity.status(201).body(java.util.Map.of("purchaseOrderId", po.getId()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> send(@PathVariable Long id) {
        try {
            var po = service.send(id);
            return ResponseEntity.ok(java.util.Map.of("purchaseOrderId", po.getId(), "status", po.getStatus()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> receive(@PathVariable Long id, @Valid @RequestBody ProcurementDtos.CreateGoodsReceipt req) {
        try {
            var receipt = service.receive(id, req.lines().stream()
                    .map(l -> new PurchaseOrderService.ReceiptLineIn(l.purchaseOrderLineId(), l.quantityReceived()))
                    .toList());
            return ResponseEntity.status(201).body(java.util.Map.of("receiptId", receipt.getId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        }
    }
}
