package com.example.crp.procurement.service;

import com.example.crp.procurement.domain.ProcurementRequest;
import com.example.crp.procurement.messaging.Events;
import com.example.crp.procurement.outbox.OutboxService;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ProcurementService {
    private final ProcurementRequestRepository repository;
    private final OutboxService outboxService;

    public ProcurementService(ProcurementRequestRepository repository, OutboxService outboxService) {
        this.repository = repository;
        this.outboxService = outboxService;
    }

    @Transactional
    public ProcurementRequest create(ProcurementRequest pr) {
        pr.setStatus("PENDING");
        pr.setCreatedAt(OffsetDateTime.now());
        ProcurementRequest saved = repository.save(pr);
        outboxService.enqueue("ProcurementRequest", saved.getId(), "procurement.requested",
                "ProcurementRequested", new Events.ProcurementRequested(saved.getId(), saved.getEquipmentId(), saved.getRequesterId()));
        return saved;
    }

    @Transactional
    public ProcurementRequest approve(Long id) {
        return repository.findById(id).map(pr -> {
            pr.setStatus("APPROVED");
            ProcurementRequest saved = repository.save(pr);
            Long approverId = null;
            outboxService.enqueue("ProcurementRequest", saved.getId(), "procurement.approved",
                    "ProcurementApproved", new Events.ProcurementApproved(saved.getId(), saved.getEquipmentId(), approverId));
            return saved;
        }).orElseThrow();
    }

    @Transactional
    public ProcurementRequest reject(Long id) {
        return repository.findById(id).map(pr -> {
            pr.setStatus("REJECTED");
            ProcurementRequest saved = repository.save(pr);
            Long approverId = null;
            outboxService.enqueue("ProcurementRequest", saved.getId(), "procurement.rejected",
                    "ProcurementRejected", new Events.ProcurementRejected(saved.getId(), saved.getEquipmentId(), approverId));
            return saved;
        }).orElseThrow();
    }
}
