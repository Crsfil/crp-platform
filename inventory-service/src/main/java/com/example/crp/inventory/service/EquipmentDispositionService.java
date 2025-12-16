package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.EquipmentDisposition;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentDispositionRepository;
import com.example.crp.inventory.repo.EquipmentLeaseRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class EquipmentDispositionService {

    private static final Set<String> OPEN_STATUSES = Set.of("DRAFT", "APPROVED", "CONTRACTED", "INVOICED", "PAID");

    private final EquipmentRepository equipmentRepository;
    private final EquipmentLeaseRepository leaseRepository;
    private final EquipmentDispositionRepository dispositionRepository;
    private final EquipmentLifecycleService lifecycleService;
    private final OutboxService outboxService;

    public EquipmentDispositionService(EquipmentRepository equipmentRepository,
                                       EquipmentLeaseRepository leaseRepository,
                                       EquipmentDispositionRepository dispositionRepository,
                                       EquipmentLifecycleService lifecycleService,
                                       OutboxService outboxService) {
        this.equipmentRepository = equipmentRepository;
        this.leaseRepository = leaseRepository;
        this.dispositionRepository = dispositionRepository;
        this.lifecycleService = lifecycleService;
        this.outboxService = outboxService;
    }

    public EquipmentDisposition get(Long id) {
        return dispositionRepository.findById(id).orElseThrow();
    }

    public List<EquipmentDisposition> list(Long equipmentId) {
        return dispositionRepository.findTop50ByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }

    @Transactional
    public EquipmentDisposition create(Long equipmentId,
                                      String type,
                                      BigDecimal plannedPrice,
                                      String currency,
                                      String counterpartyName,
                                      String counterpartyInn,
                                      Long locationId,
                                      String note,
                                      String createdBy,
                                      String correlationId) {
        if (equipmentId == null) throw new IllegalArgumentException("equipmentId is required");
        equipmentRepository.findById(equipmentId).orElseThrow();

        String t = normalizeType(type);
        if (dispositionRepository.existsByEquipmentIdAndStatusIn(equipmentId, OPEN_STATUSES)) {
            throw new IllegalStateException("Open disposition already exists for equipment");
        }
        if (leaseRepository.existsByEquipmentIdAndStatus(equipmentId, "ACTIVE")) {
            throw new IllegalStateException("Cannot dispose equipment with active lease");
        }

        EquipmentDisposition d = new EquipmentDisposition();
        d.setEquipmentId(equipmentId);
        d.setType(t);
        d.setStatus("DRAFT");
        d.setPlannedPrice(plannedPrice);
        d.setCurrency(trim(currency, 3));
        d.setCounterpartyName(trim(counterpartyName, 256));
        d.setCounterpartyInn(trim(counterpartyInn, 32));
        d.setLocationId(locationId);
        d.setNote(trim(note, 512));
        d.setCreatedBy(createdBy);
        d.setCorrelationId(correlationId);
        EquipmentDisposition saved = dispositionRepository.save(d);

        outboxService.enqueue("EquipmentDisposition", saved.getId(), "inventory.equipment.disposition_created",
                "InventoryEquipmentDispositionCreated", new Events.InventoryEquipmentDispositionCreated(
                        saved.getId(), equipmentId, t, createdBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentDisposition approve(Long dispositionId,
                                        String approvedBy,
                                        String correlationId) {
        EquipmentDisposition d = dispositionRepository.findById(dispositionId).orElseThrow();
        if (!"DRAFT".equalsIgnoreCase(d.getStatus())) {
            throw new IllegalStateException("Disposition must be DRAFT");
        }
        d.setStatus("APPROVED");
        d.setCorrelationId(correlationId);
        EquipmentDisposition saved = dispositionRepository.save(d);

        outboxService.enqueue("EquipmentDisposition", saved.getId(), "inventory.equipment.disposition_approved",
                "InventoryEquipmentDispositionApproved", new Events.InventoryEquipmentDispositionApproved(
                        saved.getId(), saved.getEquipmentId(), saved.getType(), approvedBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentDisposition contractSale(Long dispositionId,
                                            String saleMethod,
                                            String lotNumber,
                                            String contractNumber,
                                            String contractedBy,
                                            String correlationId) {
        EquipmentDisposition d = dispositionRepository.findById(dispositionId).orElseThrow();
        if (!"SALE".equalsIgnoreCase(d.getType())) {
            throw new IllegalStateException("Disposition type must be SALE");
        }
        if (!"APPROVED".equalsIgnoreCase(d.getStatus())) {
            throw new IllegalStateException("Disposition must be APPROVED");
        }
        d.setStatus("CONTRACTED");
        d.setSaleMethod(trim(saleMethod, 32));
        d.setLotNumber(trim(lotNumber, 64));
        d.setContractNumber(trim(contractNumber, 64));
        d.setCorrelationId(correlationId);
        EquipmentDisposition saved = dispositionRepository.save(d);

        outboxService.enqueue("EquipmentDisposition", saved.getId(), "inventory.equipment.sale_contracted",
                "InventoryEquipmentSaleContracted", new Events.InventoryEquipmentSaleContracted(
                        saved.getId(),
                        saved.getEquipmentId(),
                        saved.getSaleMethod(),
                        saved.getLotNumber(),
                        saved.getContractNumber(),
                        contractedBy,
                        correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentDisposition invoiceSale(Long dispositionId,
                                           String invoiceNumber,
                                           String invoicedBy,
                                           String correlationId) {
        EquipmentDisposition d = dispositionRepository.findById(dispositionId).orElseThrow();
        if (!"SALE".equalsIgnoreCase(d.getType())) {
            throw new IllegalStateException("Disposition type must be SALE");
        }
        if (!"CONTRACTED".equalsIgnoreCase(d.getStatus()) && !"APPROVED".equalsIgnoreCase(d.getStatus())) {
            throw new IllegalStateException("Disposition must be CONTRACTED or APPROVED");
        }
        d.setStatus("INVOICED");
        d.setInvoiceNumber(trim(invoiceNumber, 64));
        d.setCorrelationId(correlationId);
        EquipmentDisposition saved = dispositionRepository.save(d);
        outboxService.enqueue("EquipmentDisposition", saved.getId(), "inventory.equipment.sale_invoiced",
                "InventoryEquipmentSaleInvoiced", new Events.InventoryEquipmentSaleInvoiced(
                        saved.getId(),
                        saved.getEquipmentId(),
                        saved.getInvoiceNumber(),
                        invoicedBy,
                        correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentDisposition markPaid(Long dispositionId,
                                         OffsetDateTime paidAt,
                                         String paidBy,
                                         String correlationId) {
        EquipmentDisposition d = dispositionRepository.findById(dispositionId).orElseThrow();
        if (!"SALE".equalsIgnoreCase(d.getType())) {
            throw new IllegalStateException("Disposition type must be SALE");
        }
        if (!"INVOICED".equalsIgnoreCase(d.getStatus())) {
            throw new IllegalStateException("Disposition must be INVOICED");
        }
        d.setStatus("PAID");
        d.setPaidAt(paidAt == null ? OffsetDateTime.now() : paidAt);
        d.setCorrelationId(correlationId);
        EquipmentDisposition saved = dispositionRepository.save(d);
        outboxService.enqueue("EquipmentDisposition", saved.getId(), "inventory.equipment.sale_paid",
                "InventoryEquipmentSalePaid", new Events.InventoryEquipmentSalePaid(
                        saved.getId(),
                        saved.getEquipmentId(),
                        paidBy,
                        correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentDisposition complete(Long dispositionId,
                                         BigDecimal actualPrice,
                                         OffsetDateTime performedAt,
                                         String completedBy,
                                         String correlationId,
                                         org.springframework.security.core.Authentication auth) {
        EquipmentDisposition d = dispositionRepository.findById(dispositionId).orElseThrow();
        if ("SALE".equalsIgnoreCase(d.getType())) {
            if (!"PAID".equalsIgnoreCase(d.getStatus()) && !"APPROVED".equalsIgnoreCase(d.getStatus())) {
                throw new IllegalStateException("Sale disposition must be PAID (or APPROVED for direct sale)");
            }
        } else {
            if (!"APPROVED".equalsIgnoreCase(d.getStatus())) {
                throw new IllegalStateException("Disposition must be APPROVED");
            }
        }
        Long equipmentId = d.getEquipmentId();
        if (leaseRepository.existsByEquipmentIdAndStatus(equipmentId, "ACTIVE")) {
            throw new IllegalStateException("Cannot complete disposition with active lease");
        }

        String finalStatus = switch (normalizeType(d.getType())) {
            case "SALE" -> "SOLD";
            case "DISPOSAL" -> "DISPOSED";
            default -> throw new IllegalArgumentException("Unknown disposition type: " + d.getType());
        };

        if (d.getLocationId() != null) {
            lifecycleService.transfer(equipmentId, d.getLocationId(), null, "disposition", completedBy, correlationId, auth);
        }
        lifecycleService.changeStatus(equipmentId, finalStatus, "disposition", completedBy, correlationId, auth);

        d.setStatus("COMPLETED");
        d.setActualPrice(actualPrice);
        d.setPerformedAt(performedAt == null ? OffsetDateTime.now() : performedAt);
        d.setCorrelationId(correlationId);
        EquipmentDisposition saved = dispositionRepository.save(d);

        outboxService.enqueue("EquipmentDisposition", saved.getId(), "inventory.equipment.disposition_completed",
                "InventoryEquipmentDispositionCompleted", new Events.InventoryEquipmentDispositionCompleted(
                        saved.getId(), equipmentId, saved.getType(), finalStatus, completedBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentDisposition cancel(Long dispositionId,
                                       String canceledBy,
                                       String correlationId) {
        EquipmentDisposition d = dispositionRepository.findById(dispositionId).orElseThrow();
        if ("COMPLETED".equalsIgnoreCase(d.getStatus())) {
            throw new IllegalStateException("Disposition already COMPLETED");
        }
        d.setStatus("CANCELED");
        d.setCorrelationId(correlationId);
        EquipmentDisposition saved = dispositionRepository.save(d);

        outboxService.enqueue("EquipmentDisposition", saved.getId(), "inventory.equipment.disposition_canceled",
                "InventoryEquipmentDispositionCanceled", new Events.InventoryEquipmentDispositionCanceled(
                        saved.getId(), saved.getEquipmentId(), saved.getType(), canceledBy, correlationId
                ));
        return saved;
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("type is required");
        return type.trim().toUpperCase();
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() > max) return t.substring(0, max);
        return t;
    }
}
