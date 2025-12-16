package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.EquipmentInspection;
import com.example.crp.inventory.domain.EquipmentInspectionDocumentLink;
import com.example.crp.inventory.domain.EquipmentInspectionFinding;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentInspectionDocumentLinkRepository;
import com.example.crp.inventory.repo.EquipmentInspectionFindingRepository;
import com.example.crp.inventory.repo.EquipmentInspectionRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.LocationRepository;
import com.example.crp.inventory.security.LocationAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class EquipmentInspectionService {

    private static final Set<String> OPEN_STATUSES = Set.of("DRAFT", "SUBMITTED", "APPROVED");

    private final EquipmentRepository equipmentRepository;
    private final LocationRepository locationRepository;
    private final EquipmentInspectionRepository inspectionRepository;
    private final EquipmentInspectionFindingRepository findingRepository;
    private final EquipmentInspectionDocumentLinkRepository documentLinkRepository;
    private final EquipmentDocumentService documentService;
    private final EquipmentLifecycleService lifecycleService;
    private final LocationAccessPolicy locationAccessPolicy;
    private final OutboxService outboxService;

    public EquipmentInspectionService(EquipmentRepository equipmentRepository,
                                     LocationRepository locationRepository,
                                     EquipmentInspectionRepository inspectionRepository,
                                     EquipmentInspectionFindingRepository findingRepository,
                                     EquipmentInspectionDocumentLinkRepository documentLinkRepository,
                                     EquipmentDocumentService documentService,
                                     EquipmentLifecycleService lifecycleService,
                                     LocationAccessPolicy locationAccessPolicy,
                                     OutboxService outboxService) {
        this.equipmentRepository = equipmentRepository;
        this.locationRepository = locationRepository;
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.documentLinkRepository = documentLinkRepository;
        this.documentService = documentService;
        this.lifecycleService = lifecycleService;
        this.locationAccessPolicy = locationAccessPolicy;
        this.outboxService = outboxService;
    }

    public EquipmentInspection get(Long id) {
        return inspectionRepository.findById(id).orElseThrow();
    }

    public List<EquipmentInspection> list(Long equipmentId) {
        return inspectionRepository.findTop50ByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }

    public List<EquipmentInspectionFinding> findings(Long inspectionId) {
        return findingRepository.findByInspectionIdOrderByIdAsc(inspectionId);
    }

    public List<EquipmentInspectionDocumentLink> documents(Long inspectionId) {
        return documentLinkRepository.findByInspectionIdOrderByCreatedAtDesc(inspectionId);
    }

    @Transactional
    public EquipmentInspection create(Long equipmentId,
                                     String type,
                                     Long locationId,
                                     String summary,
                                     String createdBy,
                                     String correlationId,
                                     org.springframework.security.core.Authentication auth) {
        if (equipmentId == null) throw new IllegalArgumentException("equipmentId is required");
        equipmentRepository.findById(equipmentId).orElseThrow();

        if (inspectionRepository.existsByEquipmentIdAndStatusIn(equipmentId, OPEN_STATUSES)) {
            throw new IllegalStateException("Open inspection already exists for equipment");
        }

        String t = normalize(type, Set.of("RETURN", "PRE_SALE", "PERIODIC", "POST_REPAIR", "OTHER"), "type");
        if (locationId != null) {
            var loc = locationRepository.findById(locationId).orElseThrow();
            locationAccessPolicy.assertWriteAllowed(auth, loc);
        }

        EquipmentInspection i = new EquipmentInspection();
        i.setEquipmentId(equipmentId);
        i.setType(t);
        i.setStatus("DRAFT");
        i.setLocationId(locationId);
        i.setSummary(trim(summary, 512));
        i.setCreatedBy(createdBy);
        i.setCorrelationId(correlationId);
        EquipmentInspection saved = inspectionRepository.save(i);

        outboxService.enqueue("EquipmentInspection", saved.getId(), "inventory.equipment.inspection_created",
                "InventoryEquipmentInspectionCreated", new Events.InventoryEquipmentInspectionCreated(
                        saved.getId(), equipmentId, t, locationId, createdBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentInspectionFinding addFinding(Long inspectionId,
                                                 String code,
                                                 String severity,
                                                 String description,
                                                 BigDecimal estimatedCost) {
        EquipmentInspection i = inspectionRepository.findById(inspectionId).orElseThrow();
        if (!"DRAFT".equalsIgnoreCase(i.getStatus()) && !"SUBMITTED".equalsIgnoreCase(i.getStatus())) {
            throw new IllegalStateException("Inspection must be DRAFT or SUBMITTED");
        }

        EquipmentInspectionFinding f = new EquipmentInspectionFinding();
        f.setInspectionId(inspectionId);
        f.setCode(trim(code, 64));
        f.setSeverity(normalize(severity, Set.of("MINOR", "MAJOR", "CRITICAL"), "severity"));
        f.setDescription(trim(description, 512));
        f.setEstimatedCost(estimatedCost);
        EquipmentInspectionFinding saved = findingRepository.save(f);

        outboxService.enqueue("EquipmentInspection", inspectionId, "inventory.equipment.inspection_finding_added",
                "InventoryEquipmentInspectionFindingAdded", new Events.InventoryEquipmentInspectionFindingAdded(
                        inspectionId, saved.getId(), i.getEquipmentId(), saved.getSeverity(), saved.getCode()
                ));
        return saved;
    }

    @Transactional
    public EquipmentInspection submit(Long inspectionId,
                                     String submittedBy,
                                     String correlationId) {
        EquipmentInspection i = inspectionRepository.findById(inspectionId).orElseThrow();
        if (!"DRAFT".equalsIgnoreCase(i.getStatus())) {
            throw new IllegalStateException("Inspection must be DRAFT");
        }
        i.setStatus("SUBMITTED");
        i.setInspectedAt(OffsetDateTime.now());
        i.setCorrelationId(correlationId);
        EquipmentInspection saved = inspectionRepository.save(i);

        outboxService.enqueue("EquipmentInspection", saved.getId(), "inventory.equipment.inspection_submitted",
                "InventoryEquipmentInspectionSubmitted", new Events.InventoryEquipmentInspectionSubmitted(
                        saved.getId(), saved.getEquipmentId(), submittedBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentInspection approve(Long inspectionId,
                                      String approvedBy,
                                      String correlationId) {
        EquipmentInspection i = inspectionRepository.findById(inspectionId).orElseThrow();
        if (!"SUBMITTED".equalsIgnoreCase(i.getStatus())) {
            throw new IllegalStateException("Inspection must be SUBMITTED");
        }
        i.setStatus("APPROVED");
        i.setCorrelationId(correlationId);
        EquipmentInspection saved = inspectionRepository.save(i);

        outboxService.enqueue("EquipmentInspection", saved.getId(), "inventory.equipment.inspection_approved",
                "InventoryEquipmentInspectionApproved", new Events.InventoryEquipmentInspectionApproved(
                        saved.getId(), saved.getEquipmentId(), approvedBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentInspection complete(Long inspectionId,
                                       String conclusion,
                                       String recommendedAction,
                                       BigDecimal estimatedRepairCost,
                                       String completedBy,
                                       String correlationId,
                                       org.springframework.security.core.Authentication auth) {
        EquipmentInspection i = inspectionRepository.findById(inspectionId).orElseThrow();
        if (!"APPROVED".equalsIgnoreCase(i.getStatus())) {
            throw new IllegalStateException("Inspection must be APPROVED");
        }
        String c = normalize(conclusion, Set.of("OK", "DAMAGED", "LOST"), "conclusion");
        String ra = normalize(recommendedAction, Set.of("NONE", "REPAIR", "DISPOSITION"), "recommendedAction");

        if ("DAMAGED".equals(c)) {
            lifecycleService.changeStatus(i.getEquipmentId(), "DAMAGED", "inspection", completedBy, correlationId, auth);
        } else if ("LOST".equals(c)) {
            lifecycleService.changeStatus(i.getEquipmentId(), "LOST", "inspection", completedBy, correlationId, auth);
        }

        i.setStatus("COMPLETED");
        i.setConclusion(c);
        i.setRecommendedAction(ra);
        i.setEstimatedRepairCost(estimatedRepairCost);
        i.setInspectedAt(i.getInspectedAt() == null ? OffsetDateTime.now() : i.getInspectedAt());
        i.setCorrelationId(correlationId);
        EquipmentInspection saved = inspectionRepository.save(i);

        outboxService.enqueue("EquipmentInspection", saved.getId(), "inventory.equipment.inspection_completed",
                "InventoryEquipmentInspectionCompleted", new Events.InventoryEquipmentInspectionCompleted(
                        saved.getId(), saved.getEquipmentId(), c, ra, completedBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public EquipmentInspection cancel(Long inspectionId,
                                     String canceledBy,
                                     String correlationId) {
        EquipmentInspection i = inspectionRepository.findById(inspectionId).orElseThrow();
        if ("COMPLETED".equalsIgnoreCase(i.getStatus())) {
            throw new IllegalStateException("Inspection already COMPLETED");
        }
        i.setStatus("CANCELED");
        i.setCorrelationId(correlationId);
        return inspectionRepository.save(i);
    }

    @Transactional
    public EquipmentInspectionDocumentLink uploadDocument(Long inspectionId,
                                                         String docType,
                                                         String relationType,
                                                         MultipartFile file,
                                                         String createdBy,
                                                         String correlationId) {
        EquipmentInspection i = inspectionRepository.findById(inspectionId).orElseThrow();
        String rel = (relationType == null || relationType.isBlank()) ? "OTHER" : relationType.trim().toUpperCase();
        var doc = documentService.upload(i.getEquipmentId(), docType, file, createdBy, correlationId);

        EquipmentInspectionDocumentLink link = new EquipmentInspectionDocumentLink();
        link.setInspectionId(inspectionId);
        link.setDocumentId(doc.getId());
        link.setRelationType(rel);
        return documentLinkRepository.save(link);
    }

    private static String normalize(String raw, Set<String> allowed, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String v = raw.trim().toUpperCase();
        if (!allowed.contains(v)) {
            throw new IllegalArgumentException("Invalid " + field + ": " + v);
        }
        return v;
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() > max) return t.substring(0, max);
        return t;
    }
}
