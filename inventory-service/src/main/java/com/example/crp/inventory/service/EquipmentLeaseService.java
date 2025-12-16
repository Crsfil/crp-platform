package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentLease;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentLeaseRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class EquipmentLeaseService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentLeaseRepository leaseRepository;
    private final EquipmentLifecycleService lifecycleService;
    private final OutboxService outboxService;

    public EquipmentLeaseService(EquipmentRepository equipmentRepository,
                                EquipmentLeaseRepository leaseRepository,
                                EquipmentLifecycleService lifecycleService,
                                OutboxService outboxService) {
        this.equipmentRepository = equipmentRepository;
        this.leaseRepository = leaseRepository;
        this.lifecycleService = lifecycleService;
        this.outboxService = outboxService;
    }

    public EquipmentLease active(Long equipmentId) {
        return leaseRepository.findFirstByEquipmentIdAndStatusOrderByCreatedAtDesc(equipmentId, "ACTIVE").orElseThrow();
    }

    public List<EquipmentLease> history(Long equipmentId) {
        return leaseRepository.findTop50ByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }

    @Transactional
    public EquipmentLease start(Long equipmentId,
                               Long customerLocationId,
                               Long agreementId,
                               Long customerId,
                               OffsetDateTime expectedReturnAt,
                               String note,
                               String startedBy,
                               String correlationId,
                               org.springframework.security.core.Authentication auth) {
        if (equipmentId == null) throw new IllegalArgumentException("equipmentId is required");
        if (customerLocationId == null) throw new IllegalArgumentException("customerLocationId is required");

        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow();
        if (leaseRepository.existsByEquipmentIdAndStatus(equipmentId, "ACTIVE")) {
            throw new IllegalStateException("Active lease already exists for equipment");
        }

        Long fromLocationId = equipment.getLocationId();
        lifecycleService.transfer(equipmentId, customerLocationId, null, "lease_out", startedBy, correlationId, auth);
        lifecycleService.changeStatus(equipmentId, "LEASED", "lease_out", startedBy, correlationId, auth);

        EquipmentLease lease = new EquipmentLease();
        lease.setEquipmentId(equipmentId);
        lease.setAgreementId(agreementId);
        lease.setCustomerId(customerId);
        lease.setIssuedFromLocationId(fromLocationId);
        lease.setIssuedToLocationId(customerLocationId);
        lease.setStatus("ACTIVE");
        lease.setStartAt(OffsetDateTime.now());
        lease.setExpectedReturnAt(expectedReturnAt);
        lease.setNote(trim(note, 512));
        lease.setCreatedBy(startedBy);
        lease.setCorrelationId(correlationId);
        EquipmentLease saved = leaseRepository.save(lease);

        outboxService.enqueue("EquipmentLease", saved.getId(), "inventory.equipment.lease_started",
                "InventoryEquipmentLeaseStarted", new Events.InventoryEquipmentLeaseStarted(
                        saved.getId(),
                        equipmentId,
                        agreementId,
                        customerId,
                        fromLocationId,
                        customerLocationId,
                        startedBy,
                        correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentLease returnFromLease(Long equipmentId,
                                         Long returnLocationId,
                                         String toStatus,
                                         String note,
                                         String returnedBy,
                                         String correlationId,
                                         org.springframework.security.core.Authentication auth) {
        if (equipmentId == null) throw new IllegalArgumentException("equipmentId is required");
        if (returnLocationId == null) throw new IllegalArgumentException("returnLocationId is required");

        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow();
        EquipmentLease lease = leaseRepository.findFirstByEquipmentIdAndStatusOrderByCreatedAtDesc(equipmentId, "ACTIVE").orElseThrow();

        if (!"LEASED".equalsIgnoreCase(equipment.getStatus())) {
            throw new IllegalStateException("Equipment must be LEASED to return");
        }

        lifecycleService.transfer(equipmentId, returnLocationId, null, "lease_return", returnedBy, correlationId, auth);
        lifecycleService.changeStatus(equipmentId, "RETURNED", "lease_return", returnedBy, correlationId, auth);
        String finalStatus = (toStatus == null || toStatus.isBlank()) ? "IN_STORAGE" : toStatus.trim().toUpperCase();
        lifecycleService.changeStatus(equipmentId, finalStatus, "post_return", returnedBy, correlationId, auth);

        OffsetDateTime now = OffsetDateTime.now();
        lease.setStatus("CLOSED");
        lease.setEndAt(now);
        lease.setReturnedAt(now);
        lease.setNote(trim(note, 512));
        lease.setCorrelationId(correlationId);
        EquipmentLease saved = leaseRepository.save(lease);

        outboxService.enqueue("EquipmentLease", saved.getId(), "inventory.equipment.lease_returned",
                "InventoryEquipmentLeaseReturned", new Events.InventoryEquipmentLeaseReturned(
                        saved.getId(),
                        equipmentId,
                        returnLocationId,
                        returnedBy,
                        correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentLease repossess(Long equipmentId,
                                   Long repossessLocationId,
                                   String note,
                                   String repossessedBy,
                                   String correlationId,
                                   org.springframework.security.core.Authentication auth) {
        if (equipmentId == null) throw new IllegalArgumentException("equipmentId is required");
        if (repossessLocationId == null) throw new IllegalArgumentException("repossessLocationId is required");

        EquipmentLease lease = leaseRepository.findFirstByEquipmentIdAndStatusOrderByCreatedAtDesc(equipmentId, "ACTIVE").orElseThrow();
        lifecycleService.transfer(equipmentId, repossessLocationId, null, "repossess", repossessedBy, correlationId, auth);
        lifecycleService.changeStatus(equipmentId, "REPOSSESSED", "repossess", repossessedBy, correlationId, auth);

        OffsetDateTime now = OffsetDateTime.now();
        lease.setStatus("DEFAULTED");
        lease.setEndAt(now);
        lease.setRepossessedAt(now);
        lease.setNote(trim(note, 512));
        lease.setCorrelationId(correlationId);
        EquipmentLease saved = leaseRepository.save(lease);

        outboxService.enqueue("EquipmentLease", saved.getId(), "inventory.equipment.lease_repossessed",
                "InventoryEquipmentLeaseRepossessed", new Events.InventoryEquipmentLeaseRepossessed(
                        saved.getId(),
                        equipmentId,
                        repossessLocationId,
                        repossessedBy,
                        correlationId
                ));
        return saved;
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() > max) return t.substring(0, max);
        return t;
    }
}
