package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentRepairDocumentLink;
import com.example.crp.inventory.domain.EquipmentRepairLine;
import com.example.crp.inventory.domain.EquipmentRepairOrder;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentInspectionRepository;
import com.example.crp.inventory.repo.EquipmentRepairDocumentLinkRepository;
import com.example.crp.inventory.repo.EquipmentRepairLineRepository;
import com.example.crp.inventory.repo.EquipmentRepairOrderRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.LocationRepository;
import com.example.crp.inventory.security.LocationAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class EquipmentRepairService {

    private static final Set<String> OPEN_STATUSES = Set.of("DRAFT", "APPROVED", "IN_PROGRESS");

    private final EquipmentRepository equipmentRepository;
    private final EquipmentInspectionRepository inspectionRepository;
    private final LocationRepository locationRepository;
    private final EquipmentRepairOrderRepository repairRepository;
    private final EquipmentRepairDocumentLinkRepository documentLinkRepository;
    private final EquipmentRepairLineRepository lineRepository;
    private final EquipmentDocumentService documentService;
    private final EquipmentLifecycleService lifecycleService;
    private final EquipmentInspectionService inspectionService;
    private final LocationAccessPolicy locationAccessPolicy;
    private final OutboxService outboxService;

    public EquipmentRepairService(EquipmentRepository equipmentRepository,
                                 EquipmentInspectionRepository inspectionRepository,
                                 LocationRepository locationRepository,
                                 EquipmentRepairOrderRepository repairRepository,
                                 EquipmentRepairDocumentLinkRepository documentLinkRepository,
                                 EquipmentRepairLineRepository lineRepository,
                                 EquipmentDocumentService documentService,
                                 EquipmentLifecycleService lifecycleService,
                                 EquipmentInspectionService inspectionService,
                                 LocationAccessPolicy locationAccessPolicy,
                                 OutboxService outboxService) {
        this.equipmentRepository = equipmentRepository;
        this.inspectionRepository = inspectionRepository;
        this.locationRepository = locationRepository;
        this.repairRepository = repairRepository;
        this.documentLinkRepository = documentLinkRepository;
        this.lineRepository = lineRepository;
        this.documentService = documentService;
        this.lifecycleService = lifecycleService;
        this.inspectionService = inspectionService;
        this.locationAccessPolicy = locationAccessPolicy;
        this.outboxService = outboxService;
    }

    public EquipmentRepairOrder get(Long id) {
        return repairRepository.findById(id).orElseThrow();
    }

    public List<EquipmentRepairOrder> list(Long equipmentId) {
        return repairRepository.findTop50ByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }

    public List<EquipmentRepairDocumentLink> documents(Long repairId) {
        return documentLinkRepository.findByRepairIdOrderByCreatedAtDesc(repairId);
    }

    public List<EquipmentRepairLine> lines(Long repairId) {
        return lineRepository.findByRepairIdOrderByIdAsc(repairId);
    }

    @Transactional
    public EquipmentRepairLine addLine(Long repairId,
                                       String kind,
                                       String description,
                                       BigDecimal quantity,
                                       String uom,
                                       BigDecimal unitCost) {
        EquipmentRepairOrder r = repairRepository.findById(repairId).orElseThrow();
        if (!"DRAFT".equalsIgnoreCase(r.getStatus()) && !"APPROVED".equalsIgnoreCase(r.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Repair must be DRAFT/APPROVED/IN_PROGRESS");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        String k = (kind == null || kind.isBlank()) ? "LABOR" : kind.trim().toUpperCase();
        if (!"LABOR".equals(k) && !"PART".equals(k)) {
            throw new IllegalArgumentException("Invalid kind: " + k);
        }

        EquipmentRepairLine line = new EquipmentRepairLine();
        line.setRepairId(repairId);
        line.setKind(k);
        line.setDescription(description.trim());
        line.setQuantity(quantity);
        line.setUom(trim(uom, 16));
        line.setUnitCost(unitCost);
        if (unitCost != null) {
            BigDecimal total = unitCost.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            line.setTotalCost(total);
        }
        EquipmentRepairLine saved = lineRepository.save(line);

        // keep order.actualCost aligned to sum(lines) when not completed yet
        BigDecimal sum = lineRepository.sumTotalCostByRepairId(repairId);
        r.setActualCost(sum);
        repairRepository.save(r);

        outboxService.enqueue("EquipmentRepair", repairId, "inventory.equipment.repair_line_added",
                "InventoryEquipmentRepairLineAdded", new Events.InventoryEquipmentRepairLineAdded(
                        repairId, saved.getId(), r.getEquipmentId(), saved.getKind(), saved.getTotalCost()
                ));
        return saved;
    }

    @Transactional
    public EquipmentRepairOrder create(Long equipmentId,
                                      Long inspectionId,
                                      Long repairLocationId,
                                      String vendorName,
                                      String vendorInn,
                                      BigDecimal plannedCost,
                                      String currency,
                                      String note,
                                      String createdBy,
                                      String correlationId,
                                      org.springframework.security.core.Authentication auth) {
        if (equipmentId == null) throw new IllegalArgumentException("equipmentId is required");
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow();

        if (repairRepository.existsByEquipmentIdAndStatusIn(equipmentId, OPEN_STATUSES)) {
            throw new IllegalStateException("Open repair already exists for equipment");
        }
        if (inspectionId != null) {
            inspectionRepository.findById(inspectionId).orElseThrow();
        }
        if (repairLocationId != null) {
            var loc = locationRepository.findById(repairLocationId).orElseThrow();
            locationAccessPolicy.assertWriteAllowed(auth, loc);
        }

        EquipmentRepairOrder r = new EquipmentRepairOrder();
        r.setEquipmentId(equipmentId);
        r.setInspectionId(inspectionId);
        r.setStatus("DRAFT");
        r.setRepairLocationId(repairLocationId);
        r.setVendorName(trim(vendorName, 256));
        r.setVendorInn(trim(vendorInn, 32));
        r.setPlannedCost(plannedCost);
        r.setCurrency(trim(currency, 3));
        r.setNote(trim(note, 512));
        r.setCreatedBy(createdBy);
        r.setCorrelationId(correlationId);
        EquipmentRepairOrder saved = repairRepository.save(r);

        // if the asset isn't marked as DAMAGED yet, this workflow still can be used, but usually it's DAMAGED.
        outboxService.enqueue("EquipmentRepair", saved.getId(), "inventory.equipment.repair_created",
                "InventoryEquipmentRepairCreated", new Events.InventoryEquipmentRepairCreated(
                        saved.getId(), equipmentId, inspectionId, repairLocationId, createdBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentRepairOrder approve(Long repairId,
                                       String approvedBy,
                                       String correlationId) {
        EquipmentRepairOrder r = repairRepository.findById(repairId).orElseThrow();
        if (!"DRAFT".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Repair must be DRAFT");
        }
        r.setStatus("APPROVED");
        r.setCorrelationId(correlationId);
        EquipmentRepairOrder saved = repairRepository.save(r);
        outboxService.enqueue("EquipmentRepair", saved.getId(), "inventory.equipment.repair_approved",
                "InventoryEquipmentRepairApproved", new Events.InventoryEquipmentRepairApproved(
                        saved.getId(), saved.getEquipmentId(), approvedBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentRepairOrder start(Long repairId,
                                    OffsetDateTime startedAt,
                                    String startedBy,
                                    String correlationId,
                                    org.springframework.security.core.Authentication auth) {
        EquipmentRepairOrder r = repairRepository.findById(repairId).orElseThrow();
        if (!"APPROVED".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Repair must be APPROVED");
        }

        Long equipmentId = r.getEquipmentId();
        if (r.getRepairLocationId() != null) {
            lifecycleService.transfer(equipmentId, r.getRepairLocationId(), null, "repair", startedBy, correlationId, auth);
        }
        lifecycleService.changeStatus(equipmentId, "IN_STORAGE", "repair_started", startedBy, correlationId, auth);

        r.setStatus("IN_PROGRESS");
        r.setStartedAt(startedAt == null ? OffsetDateTime.now() : startedAt);
        r.setCorrelationId(correlationId);
        EquipmentRepairOrder saved = repairRepository.save(r);

        outboxService.enqueue("EquipmentRepair", saved.getId(), "inventory.equipment.repair_started",
                "InventoryEquipmentRepairStarted", new Events.InventoryEquipmentRepairStarted(
                        saved.getId(), saved.getEquipmentId(), startedBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentRepairOrder complete(Long repairId,
                                       BigDecimal actualCost,
                                       OffsetDateTime completedAt,
                                       boolean markAvailable,
                                       boolean createPostRepairInspection,
                                       String note,
                                       String completedBy,
                                       String correlationId,
                                       org.springframework.security.core.Authentication auth) {
        EquipmentRepairOrder r = repairRepository.findById(repairId).orElseThrow();
        if (!"IN_PROGRESS".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Repair must be IN_PROGRESS");
        }

        Long equipmentId = r.getEquipmentId();
        String next = markAvailable ? "AVAILABLE" : "IN_STORAGE";
        lifecycleService.changeStatus(equipmentId, next, "repair_completed", completedBy, correlationId, auth);

        r.setStatus("COMPLETED");
        BigDecimal cost = actualCost != null ? actualCost : lineRepository.sumTotalCostByRepairId(repairId);
        r.setActualCost(cost);
        r.setCompletedAt(completedAt == null ? OffsetDateTime.now() : completedAt);
        r.setNote(trim(note, 512));
        r.setCorrelationId(correlationId);
        EquipmentRepairOrder saved = repairRepository.save(r);

        outboxService.enqueue("EquipmentRepair", saved.getId(), "inventory.equipment.repair_completed",
                "InventoryEquipmentRepairCompleted", new Events.InventoryEquipmentRepairCompleted(
                        saved.getId(), saved.getEquipmentId(), completedBy, correlationId
                ));

        if (createPostRepairInspection) {
            try {
                var inspection = inspectionService.create(
                        equipmentId,
                        "POST_REPAIR",
                        saved.getRepairLocationId(),
                        "Post repair inspection for repairId=" + saved.getId(),
                        completedBy,
                        correlationId,
                        auth
                );
                outboxService.enqueue("EquipmentRepair", saved.getId(), "inventory.equipment.repair_post_inspection_created",
                        "InventoryEquipmentRepairPostInspectionCreated", new Events.InventoryEquipmentRepairPostInspectionCreated(
                                saved.getId(), inspection.getId(), equipmentId, completedBy, correlationId
                        ));
            } catch (RuntimeException ignored) {
                // best-effort: repair completion shouldn't rollback due to inspection creation
            }
        }
        return saved;
    }

    @Transactional
    public EquipmentRepairOrder cancel(Long repairId,
                                      String note,
                                      String canceledBy,
                                      String correlationId) {
        EquipmentRepairOrder r = repairRepository.findById(repairId).orElseThrow();
        if ("COMPLETED".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Repair already COMPLETED");
        }
        r.setStatus("CANCELED");
        r.setNote(trim(note, 512));
        r.setCorrelationId(correlationId);
        EquipmentRepairOrder saved = repairRepository.save(r);

        outboxService.enqueue("EquipmentRepair", saved.getId(), "inventory.equipment.repair_canceled",
                "InventoryEquipmentRepairCanceled", new Events.InventoryEquipmentRepairCanceled(
                        saved.getId(), saved.getEquipmentId(), canceledBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentRepairDocumentLink uploadDocument(Long repairId,
                                                     String docType,
                                                     String relationType,
                                                     MultipartFile file,
                                                     String createdBy,
                                                     String correlationId) {
        EquipmentRepairOrder r = repairRepository.findById(repairId).orElseThrow();
        Equipment docOwner = equipmentRepository.findById(r.getEquipmentId()).orElseThrow();
        var doc = documentService.upload(docOwner.getId(), docType, file, createdBy, correlationId);
        String rel = (relationType == null || relationType.isBlank()) ? "OTHER" : relationType.trim().toUpperCase();

        EquipmentRepairDocumentLink link = new EquipmentRepairDocumentLink();
        link.setRepairId(repairId);
        link.setDocumentId(doc.getId());
        link.setRelationType(rel);
        EquipmentRepairDocumentLink saved = documentLinkRepository.save(link);

        outboxService.enqueue("EquipmentRepair", repairId, "inventory.equipment.repair_document_linked",
                "InventoryEquipmentRepairDocumentLinked", new Events.InventoryEquipmentRepairDocumentLinked(
                        repairId, r.getEquipmentId(), doc.getId().toString(), rel, createdBy, correlationId
                ));
        return saved;
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() > max) return t.substring(0, max);
        return t;
    }
}
