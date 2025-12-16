package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.EquipmentDispositionDocumentLink;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentDispositionDocumentLinkRepository;
import com.example.crp.inventory.repo.EquipmentDispositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class EquipmentDispositionDocumentsService {

    private final EquipmentDispositionRepository dispositionRepository;
    private final EquipmentDispositionDocumentLinkRepository linkRepository;
    private final EquipmentDocumentService documentService;
    private final OutboxService outboxService;

    public EquipmentDispositionDocumentsService(EquipmentDispositionRepository dispositionRepository,
                                               EquipmentDispositionDocumentLinkRepository linkRepository,
                                               EquipmentDocumentService documentService,
                                               OutboxService outboxService) {
        this.dispositionRepository = dispositionRepository;
        this.linkRepository = linkRepository;
        this.documentService = documentService;
        this.outboxService = outboxService;
    }

    public List<EquipmentDispositionDocumentLink> list(Long dispositionId) {
        return linkRepository.findByDispositionIdOrderByCreatedAtDesc(dispositionId);
    }

    @Transactional
    public EquipmentDispositionDocumentLink upload(Long dispositionId,
                                                   String docType,
                                                   String relationType,
                                                   MultipartFile file,
                                                   String createdBy,
                                                   String correlationId) {
        var disposition = dispositionRepository.findById(dispositionId).orElseThrow();
        var doc = documentService.upload(disposition.getEquipmentId(), docType, file, createdBy, correlationId);
        String rel = (relationType == null || relationType.isBlank()) ? "OTHER" : relationType.trim().toUpperCase();

        EquipmentDispositionDocumentLink link = new EquipmentDispositionDocumentLink();
        link.setDispositionId(dispositionId);
        link.setDocumentId(doc.getId());
        link.setRelationType(rel);
        EquipmentDispositionDocumentLink saved = linkRepository.save(link);

        outboxService.enqueue("EquipmentDisposition", dispositionId, "inventory.equipment.disposition_document_linked",
                "InventoryEquipmentDispositionDocumentLinked", new Events.InventoryEquipmentDispositionDocumentLinked(
                        dispositionId,
                        disposition.getEquipmentId(),
                        doc.getId().toString(),
                        rel,
                        createdBy,
                        correlationId
                ));
        return saved;
    }
}

