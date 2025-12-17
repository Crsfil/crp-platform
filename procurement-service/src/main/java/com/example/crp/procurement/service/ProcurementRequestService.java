package com.example.crp.procurement.service;

import com.example.crp.procurement.domain.ProcurementRequest;
import com.example.crp.procurement.domain.ProcurementRequestLine;
import com.example.crp.procurement.messaging.Events;
import com.example.crp.procurement.outbox.OutboxService;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ProcurementRequestService {
    private final ProcurementRequestRepository repository;
    private final OutboxService outboxService;

    public ProcurementRequestService(ProcurementRequestRepository repository, OutboxService outboxService) {
        this.repository = repository;
        this.outboxService = outboxService;
    }

    public List<ProcurementRequest> list() {
        return repository.findAll();
    }

    @Transactional
    public ProcurementRequest create(ProcurementRequest pr) {
        String kind = pr.getKind();
        if (kind == null || kind.isBlank()) {
            kind = "PURCHASE";
        }
        kind = kind.trim().toUpperCase();
        // Allow both товарные закупки и сервисные под изъятие/хранение/оценку/ремонт/аукцион
        var allowedKinds = java.util.Set.of("PURCHASE", "STOCK_RESERVATION",
                "SERVICE_EVICTION", "SERVICE_STORAGE", "SERVICE_VALUATION", "SERVICE_REPAIR", "SERVICE_AUCTION");
        if (!allowedKinds.contains(kind)) {
            throw new IllegalArgumentException("Unsupported request kind: " + kind);
        }
        pr.setKind(kind);
        pr.setStatus("SUBMITTED");
        pr.setCreatedAt(OffsetDateTime.now());

        if (pr.getLines() != null) {
            for (ProcurementRequestLine line : pr.getLines()) {
                line.setRequest(pr);
            }
        }
        pr.setAmount(calculateAmount(pr));

        ProcurementRequest saved = repository.save(pr);
        if (saved.getRequestNumber() == null || saved.getRequestNumber().isBlank()) {
            saved.setRequestNumber("PR-" + saved.getId());
            repository.save(saved);
        }

        outboxService.enqueue("ProcurementRequest", saved.getId(), "procurement.requested",
                "ProcurementRequested", new Events.ProcurementRequested(saved.getId(), saved.getEquipmentId(), saved.getRequesterId()));
        return saved;
    }

    @Transactional
    public ProcurementRequest approve(Long id, String approvedBy) {
        return repository.findWithLinesById(id).map(pr -> {
            pr.setStatus("APPROVED");
            pr.setApprovedAt(OffsetDateTime.now());
            pr.setApprovedBy(approvedBy);
            pr.setAmount(calculateAmount(pr));
            ProcurementRequest saved = repository.save(pr);
            if (saved.getEquipmentId() != null) {
                Long approverId = null;
                outboxService.enqueue("ProcurementRequest", saved.getId(), "procurement.approved",
                        "ProcurementApproved", new Events.ProcurementApproved(saved.getId(), saved.getEquipmentId(), approverId));
            }
            return saved;
        }).orElseThrow();
    }

    @Transactional
    public ProcurementRequest reject(Long id, String approvedBy) {
        return repository.findById(id).map(pr -> {
            pr.setStatus("REJECTED");
            pr.setApprovedAt(OffsetDateTime.now());
            pr.setApprovedBy(approvedBy);
            ProcurementRequest saved = repository.save(pr);
            if (saved.getEquipmentId() != null) {
                Long approverId = null;
                outboxService.enqueue("ProcurementRequest", saved.getId(), "procurement.rejected",
                        "ProcurementRejected", new Events.ProcurementRejected(saved.getId(), saved.getEquipmentId(), approverId));
            }
            return saved;
        }).orElseThrow();
    }

    private static BigDecimal calculateAmount(ProcurementRequest pr) {
        if (pr.getLines() == null || pr.getLines().isEmpty()) {
            return pr.getAmount();
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ProcurementRequestLine l : pr.getLines()) {
            if (l.getUnitPrice() != null && l.getQuantity() != null) {
                total = total.add(l.getUnitPrice().multiply(l.getQuantity()));
            }
        }
        return total;
    }
}
