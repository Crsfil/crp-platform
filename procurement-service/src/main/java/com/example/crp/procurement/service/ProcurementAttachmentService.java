package com.example.crp.procurement.service;

import com.example.crp.procurement.domain.ProcurementAttachment;
import com.example.crp.procurement.repo.GoodsReceiptRepository;
import com.example.crp.procurement.repo.ProcurementAttachmentRepository;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import com.example.crp.procurement.repo.PurchaseOrderRepository;
import com.example.crp.procurement.storage.AttachmentStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcurementAttachmentService {

    public static final String OWNER_REQUEST = "REQUEST";
    public static final String OWNER_PURCHASE_ORDER = "PURCHASE_ORDER";
    public static final String OWNER_GOODS_RECEIPT = "GOODS_RECEIPT";

    private final ProcurementAttachmentRepository repository;
    private final ProcurementRequestRepository requestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final AttachmentStorage storage;

    public ProcurementAttachmentService(ProcurementAttachmentRepository repository,
                                        ProcurementRequestRepository requestRepository,
                                        PurchaseOrderRepository purchaseOrderRepository,
                                        GoodsReceiptRepository goodsReceiptRepository,
                                        AttachmentStorage storage) {
        this.repository = repository;
        this.requestRepository = requestRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.storage = storage;
    }

    public List<ProcurementAttachment> list(String ownerType, Long ownerId) {
        return repository.findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(ownerType, ownerId);
    }

    @Transactional
    public ProcurementAttachment upload(String ownerType, Long ownerId, MultipartFile file, String createdBy) {
        ensureOwnerExists(ownerType, ownerId);
        try {
            String fileName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String contentType = file.getContentType();
            byte[] bytes = file.getBytes();
            AttachmentStorage.StoredObject stored = storage.put(ownerType + "-" + ownerId, fileName, contentType, bytes);

            ProcurementAttachment a = new ProcurementAttachment();
            a.setOwnerType(ownerType);
            a.setOwnerId(ownerId);
            a.setFileName(stored.fileName());
            a.setContentType(stored.contentType());
            a.setFileSize(stored.sizeBytes());
            a.setSha256(stored.sha256Hex());
            a.setStorageType(stored.storageType());
            a.setStorageLocation(stored.location());
            a.setCreatedBy(createdBy);
            return repository.save(a);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload attachment", e);
        }
    }

    public byte[] download(UUID attachmentId) {
        ProcurementAttachment a = repository.findById(attachmentId).orElseThrow();
        return storage.get(new AttachmentStorage.StoredObjectRef(a.getStorageType(), a.getStorageLocation()));
    }

    public ProcurementAttachment get(UUID attachmentId) {
        return repository.findById(attachmentId).orElseThrow();
    }

    public Optional<URI> downloadUrl(UUID attachmentId) {
        ProcurementAttachment a = repository.findById(attachmentId).orElseThrow();
        return storage.presignGet(new AttachmentStorage.StoredObjectRef(a.getStorageType(), a.getStorageLocation()));
    }

    private void ensureOwnerExists(String ownerType, Long ownerId) {
        if (OWNER_REQUEST.equals(ownerType)) {
            requestRepository.findById(ownerId).orElseThrow();
            return;
        }
        if (OWNER_PURCHASE_ORDER.equals(ownerType)) {
            purchaseOrderRepository.findById(ownerId).orElseThrow();
            return;
        }
        if (OWNER_GOODS_RECEIPT.equals(ownerType)) {
            goodsReceiptRepository.findById(ownerId).orElseThrow();
            return;
        }
        throw new IllegalArgumentException("Unknown ownerType: " + ownerType);
    }
}
