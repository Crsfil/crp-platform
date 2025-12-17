package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.EquipmentDocument;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentDocumentRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.storage.DocumentStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EquipmentDocumentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentDocumentRepository repository;
    private final DocumentStorage storage;
    private final OutboxService outboxService;

    public EquipmentDocumentService(EquipmentRepository equipmentRepository,
                                   EquipmentDocumentRepository repository,
                                   DocumentStorage storage,
                                   OutboxService outboxService) {
        this.equipmentRepository = equipmentRepository;
        this.repository = repository;
        this.storage = storage;
        this.outboxService = outboxService;
    }

    public List<EquipmentDocument> list(Long equipmentId) {
        return repository.findByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }

    public EquipmentDocument get(UUID id) {
        return repository.findById(id).orElseThrow();
    }

    public byte[] download(UUID id) {
        EquipmentDocument doc = repository.findById(id).orElseThrow();
        return storage.get(new DocumentStorage.StoredObjectRef(doc.getStorageType(), doc.getStorageLocation()));
    }

    public Optional<URI> downloadUrl(UUID id) {
        EquipmentDocument doc = repository.findById(id).orElseThrow();
        return storage.presignGet(new DocumentStorage.StoredObjectRef(doc.getStorageType(), doc.getStorageLocation()));
    }

    @Transactional
    public EquipmentDocument upload(Long equipmentId,
                                   String docType,
                                   MultipartFile file,
                                   String createdBy,
                                   String correlationId) {
        if (equipmentId == null) {
            throw new IllegalArgumentException("equipmentId is required");
        }
        equipmentRepository.findById(equipmentId).orElseThrow();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
        String originalName = file.getOriginalFilename() == null ? "file.bin" : file.getOriginalFilename();
        String contentType = file.getContentType();
        long maxBytes = 15 * 1024 * 1024L; // 15MB
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("file too large (max 15MB)");
        }
        if (contentType != null && contentType.length() > 128) {
            contentType = contentType.substring(0, 128);
        }

        DocumentStorage.StoredObject stored = storage.put("equipment-" + equipmentId, originalName, contentType, bytes);

        String normalizedDocType = (docType == null || docType.isBlank()) ? "GENERIC" : docType.trim().toUpperCase();
        List<String> allowed = java.util.List.of(
                "GENERIC",
                "EQUIPMENT_PASSPORT",
                "PASSPORT",
                "PHOTO",
                "EVACUATION_ACT",
                "STORAGE_ACT",
                "VALUATION_REPORT",
                "REPAIR_REPORT",
                "AUCTION_REPORT",
                "SALE_CONTRACT",
                "INVOICE",
                "PAYMENT_CONFIRMATION"
        );
        if (!allowed.contains(normalizedDocType)) {
            throw new IllegalArgumentException("unsupported docType");
        }

        EquipmentDocument doc = new EquipmentDocument();
        doc.setId(UUID.randomUUID());
        doc.setEquipmentId(equipmentId);
        doc.setDocType(normalizedDocType);
        doc.setFileName(stored.fileName());
        doc.setContentType(stored.contentType());
        doc.setSizeBytes(stored.sizeBytes());
        doc.setSha256(stored.sha256Hex());
        doc.setStorageType(stored.storageType());
        doc.setStorageLocation(stored.location());
        doc.setCreatedBy(createdBy);
        EquipmentDocument saved = repository.save(doc);

        outboxService.enqueue("Equipment", equipmentId, "inventory.equipment.document_uploaded",
                "InventoryEquipmentDocumentUploaded", new Events.InventoryEquipmentDocumentUploaded(
                        saved.getId().toString(),
                        equipmentId,
                        saved.getDocType(),
                        saved.getFileName(),
                        saved.getStorageType(),
                        saved.getStorageLocation(),
                        createdBy,
                        correlationId
                ));
        return saved;
    }

    public static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
