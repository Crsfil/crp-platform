package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.*;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class EquipmentRepossessionService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentRepossessionCaseRepository repossessionCaseRepository;
    private final EquipmentStorageOrderRepository storageOrderRepository;
    private final EquipmentValuationRepository valuationRepository;
    private final EquipmentCustodyHistoryRepository custodyHistoryRepository;
    private final LocationRepository locationRepository;
    private final EquipmentLifecycleService lifecycleService;
    private final OutboxService outboxService;

    public EquipmentRepossessionService(EquipmentRepository equipmentRepository,
                                        EquipmentRepossessionCaseRepository repossessionCaseRepository,
                                        EquipmentStorageOrderRepository storageOrderRepository,
                                        EquipmentValuationRepository valuationRepository,
                                        EquipmentCustodyHistoryRepository custodyHistoryRepository,
                                        LocationRepository locationRepository,
                                        EquipmentLifecycleService lifecycleService,
                                        OutboxService outboxService) {
        this.equipmentRepository = equipmentRepository;
        this.repossessionCaseRepository = repossessionCaseRepository;
        this.storageOrderRepository = storageOrderRepository;
        this.valuationRepository = valuationRepository;
        this.custodyHistoryRepository = custodyHistoryRepository;
        this.locationRepository = locationRepository;
        this.lifecycleService = lifecycleService;
        this.outboxService = outboxService;
    }

    @Transactional
    public void handleServiceCompleted(com.example.crp.inventory.messaging.Events.ProcurementServiceCompleted msg) {
        if (msg == null || msg.equipmentId() == null) {
            return;
        }
        EquipmentStorageOrder storageOrder = null;
        if (msg.serviceOrderId() != null) {
            storageOrder = storageOrderRepository.findFirstByProcurementServiceOrderId(msg.serviceOrderId()).orElse(null);
        }
        if (storageOrder == null) {
            storageOrder = storageOrderRepository.findFirstByEquipmentIdAndStatusOrderByCreatedAtDesc(msg.equipmentId(), "IN_PROGRESS").orElse(null);
        }
        if (storageOrder != null && "SERVICE_STORAGE".equalsIgnoreCase(msg.serviceType())) {
            if (storageOrder.getProcurementServiceOrderId() == null && msg.serviceOrderId() != null) {
                storageOrder.setProcurementServiceOrderId(msg.serviceOrderId());
            }
            storageOrder.setStatus("COMPLETED");
            storageOrder.setActualCost(msg.actualCost());
            storageOrder.setReleasedAt(msg.completedAt());
            storageOrder.setCorrelationId(storageOrder.getCorrelationId());
            storageOrderRepository.save(storageOrder);
        }
        if ("SERVICE_VALUATION".equalsIgnoreCase(msg.serviceType())) {
            tryChangeStatus(msg.equipmentId(), EquipmentStatus.UNDER_EVALUATION.name(), "service_completed", null, null, null);
        } else if ("SERVICE_REPAIR".equalsIgnoreCase(msg.serviceType())) {
            tryChangeStatus(msg.equipmentId(), EquipmentStatus.UNDER_REPAIR.name(), "service_completed", null, null, null);
        } else if ("SERVICE_AUCTION".equalsIgnoreCase(msg.serviceType())) {
            tryChangeStatus(msg.equipmentId(), EquipmentStatus.SALE_LISTED.name(), "service_completed", null, null, null);
        }
    }

    @Transactional
    public void handleServiceCreated(com.example.crp.inventory.messaging.Events.ProcurementServiceCreated msg) {
        if (msg == null || msg.equipmentId() == null || msg.serviceOrderId() == null) {
            return;
        }
        if (!"SERVICE_STORAGE".equalsIgnoreCase(msg.serviceType())) {
            return;
        }
        EquipmentStorageOrder storageOrder = storageOrderRepository.findFirstByProcurementServiceOrderId(msg.serviceOrderId()).orElse(null);
        if (storageOrder == null) {
            storageOrder = storageOrderRepository.findFirstByEquipmentIdAndStatusOrderByCreatedAtDesc(msg.equipmentId(), "IN_PROGRESS").orElse(null);
        }
        if (storageOrder == null) {
            return;
        }
        if (storageOrder.getProcurementServiceOrderId() == null) {
            storageOrder.setProcurementServiceOrderId(msg.serviceOrderId());
            storageOrderRepository.save(storageOrder);
        }
    }

    @Transactional
    public EquipmentRepossessionCase openCase(Long equipmentId,
                                              String triggerReason,
                                              String decisionRef,
                                              Long targetLocationId,
                                              String initiatedBy,
                                              String correlationId,
                                              org.springframework.security.core.Authentication auth) {
        if (equipmentId == null) {
            throw new IllegalArgumentException("equipmentId is required");
        }
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow();
        if (repossessionCaseRepository.existsByEquipmentIdAndStatusIn(equipmentId, List.of("PENDING", "IN_PROGRESS"))) {
            throw new IllegalStateException("Active repossession case already exists");
        }
        if (targetLocationId != null && locationRepository.findById(targetLocationId).isEmpty()) {
            throw new IllegalArgumentException("target location not found");
        }

        EquipmentRepossessionCase c = new EquipmentRepossessionCase();
        c.setEquipmentId(equipmentId);
        c.setStatus("PENDING");
        c.setTriggerReason(trim(triggerReason, 256));
        c.setDecisionRef(trim(decisionRef, 128));
        c.setTargetLocationId(targetLocationId);
        c.setInitiatedBy(initiatedBy);
        c.setCorrelationId(correlationId);
        EquipmentRepossessionCase saved = repossessionCaseRepository.save(c);

        equipment.setRepossessionCaseId(saved.getId());
        equipmentRepository.save(equipment);

        // status shift to REPOSSESSION_PENDING if allowed
        lifecycleService.changeStatus(equipmentId, EquipmentStatus.REPOSSESSION_PENDING.name(), "repossess_case", initiatedBy, correlationId, auth);

        outboxService.enqueue("EquipmentRepossessionCase", saved.getId(), "inventory.repossession.started",
                "InventoryRepossessionStarted", new Events.InventoryRepossessionStarted(
                        saved.getId(), equipmentId, targetLocationId, initiatedBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentStorageOrder intakeToStorage(Long equipmentId,
                                                 Long storageLocationId,
                                                 String vendorName,
                                                 String vendorInn,
                                                 OffsetDateTime slaUntil,
                                                 BigDecimal expectedCost,
                                                 String currency,
                                                 Long procurementServiceOrderId,
                                                 String note,
                                                 String createdBy,
                                                 String correlationId,
                                                 org.springframework.security.core.Authentication auth) {
        if (equipmentId == null) throw new IllegalArgumentException("equipmentId is required");
        if (storageLocationId == null) throw new IllegalArgumentException("storageLocationId is required");

        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow();
        var storageLocation = locationRepository.findById(storageLocationId).orElseThrow();

        EquipmentStorageOrder order = new EquipmentStorageOrder();
        order.setEquipmentId(equipmentId);
        order.setStorageLocationId(storageLocationId);
        order.setVendorName(trim(vendorName, 256));
        order.setVendorInn(trim(vendorInn, 32));
        order.setSlaUntil(slaUntil);
        order.setExpectedCost(expectedCost);
        order.setCurrency(trim(currency, 3));
        order.setProcurementServiceOrderId(procurementServiceOrderId);
        order.setNote(trim(note, 512));
        order.setCreatedBy(createdBy);
        order.setCorrelationId(correlationId);
        EquipmentStorageOrder saved = storageOrderRepository.save(order);

        lifecycleService.transfer(equipmentId, storageLocation.getId(), null, "storage_intake", createdBy, correlationId, auth);
        lifecycleService.changeStatus(equipmentId, EquipmentStatus.IN_STORAGE.name(), "storage_intake", createdBy, correlationId, auth);

        EquipmentCustodyHistory custody = new EquipmentCustodyHistory();
        custody.setEquipmentId(equipmentId);
        custody.setLocationId(storageLocationId);
        custody.setCustodian(storageLocation.getCode());
        custody.setReason("storage");
        custodyHistoryRepository.save(custody);

        equipment.setCurrentCustodian(storageLocation.getCode());
        equipmentRepository.save(equipment);

        outboxService.enqueue("EquipmentStorageOrder", saved.getId(), "inventory.storage.accepted",
                "InventoryStorageAccepted", new Events.InventoryStorageAccepted(
                        saved.getId(), equipmentId, storageLocationId, createdBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentValuation recordValuation(Long equipmentId,
                                              java.math.BigDecimal valuationAmount,
                                              java.math.BigDecimal liquidationAmount,
                                              String currency,
                                              OffsetDateTime valuatedAt,
                                              String vendorName,
                                              String vendorInn,
                                              String note,
                                              String createdBy,
                                              String correlationId,
                                              org.springframework.security.core.Authentication auth) {
        if (equipmentId == null) throw new IllegalArgumentException("equipmentId is required");
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow();

        EquipmentValuation v = new EquipmentValuation();
        v.setEquipmentId(equipmentId);
        v.setValuationAmount(valuationAmount);
        v.setLiquidationAmount(liquidationAmount);
        v.setCurrency(trim(currency, 3));
        v.setValuatedAt(valuatedAt);
        v.setVendorName(trim(vendorName, 256));
        v.setVendorInn(trim(vendorInn, 32));
        v.setNote(trim(note, 512));
        v.setCreatedBy(createdBy);
        v.setCorrelationId(correlationId);
        EquipmentValuation saved = valuationRepository.save(v);

        equipment.setValuationAmount(valuationAmount);
        equipment.setValuationCurrency(trim(currency, 3));
        equipment.setValuationAt(saved.getValuatedAt());
        equipmentRepository.save(equipment);

        tryChangeStatus(equipmentId, EquipmentStatus.UNDER_EVALUATION.name(), "valuation", createdBy, correlationId, auth);

        outboxService.enqueue("EquipmentValuation", saved.getId(), "inventory.valuation.recorded",
                "InventoryValuationRecorded", new Events.InventoryValuationRecorded(
                        saved.getId(), equipmentId, valuationAmount, liquidationAmount, saved.getCurrency(), createdBy, correlationId
                ));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<EquipmentRepossessionCase> listCases(Long equipmentId) {
        return repossessionCaseRepository.findTop20ByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentStorageOrder> listStorageOrders(Long equipmentId) {
        return storageOrderRepository.findTop50ByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentValuation> listValuations(Long equipmentId) {
        return valuationRepository.findTop20ByEquipmentIdOrderByValuatedAtDesc(equipmentId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentCustodyHistory> listCustody(Long equipmentId) {
        return custodyHistoryRepository.findTop50ByEquipmentIdOrderByFromTsDesc(equipmentId);
    }

    private void tryChangeStatus(Long equipmentId,
                                 String toStatus,
                                 String reason,
                                 String changedBy,
                                 String correlationId,
                                 org.springframework.security.core.Authentication auth) {
        try {
            lifecycleService.changeStatus(equipmentId, toStatus, reason, changedBy, correlationId, auth);
        } catch (IllegalStateException ignored) {
            // keep current if transition not allowed; we intentionally don't fail intake/valuation
        }
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }
}
