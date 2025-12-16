package com.example.crp.procurement.web;

import com.example.crp.procurement.repo.GoodsReceiptRepository;
import com.example.crp.procurement.service.ProcurementAttachmentService;
import com.example.crp.procurement.service.PurchaseOrderService;
import com.example.crp.procurement.web.dto.ProcurementDtos;
import com.example.crp.procurement.web.dto.ProcurementMappers;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/receipts")
public class GoodsReceiptsController {
    private final GoodsReceiptRepository repository;
    private final PurchaseOrderService service;
    private final ProcurementAttachmentService attachmentService;

    public GoodsReceiptsController(GoodsReceiptRepository repository,
                                   PurchaseOrderService service,
                                   ProcurementAttachmentService attachmentService) {
        this.repository = repository;
        this.service = service;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<ProcurementDtos.GoodsReceiptSummary> list() {
        return repository.findAll().stream().map(ProcurementMappers::toReceiptSummary).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<ProcurementDtos.GoodsReceiptDto> get(@PathVariable Long id) {
        return repository.findWithLinesById(id)
                .map(gr -> ResponseEntity.ok(ProcurementMappers.toReceipt(gr, attachmentService.list(ProcurementAttachmentService.OWNER_GOODS_RECEIPT, gr.getId()))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> accept(@PathVariable Long id) {
        try {
            var gr = service.acceptReceipt(id);
            return ResponseEntity.ok(java.util.Map.of("receiptId", gr.getId(), "status", gr.getStatus()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        try {
            var gr = service.rejectReceipt(id);
            return ResponseEntity.ok(java.util.Map.of("receiptId", gr.getId(), "status", gr.getStatus()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
