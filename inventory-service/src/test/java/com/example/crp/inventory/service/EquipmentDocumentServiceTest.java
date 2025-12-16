package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentDocument;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentDocumentRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.storage.DocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EquipmentDocumentServiceTest {

    private EquipmentRepository equipmentRepository;
    private EquipmentDocumentRepository repository;
    private DocumentStorage storage;
    private OutboxService outboxService;
    private EquipmentDocumentService service;

    @BeforeEach
    void setUp() {
        equipmentRepository = mock(EquipmentRepository.class);
        repository = mock(EquipmentDocumentRepository.class);
        storage = mock(DocumentStorage.class);
        outboxService = mock(OutboxService.class);
        service = new EquipmentDocumentService(equipmentRepository, repository, storage, outboxService);
    }

    @Test
    void upload_storesBytes_persistsMetadata_andEnqueuesOutbox() {
        Equipment e = new Equipment();
        e.setId(10L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e));
        when(storage.put(anyString(), anyString(), any(), any())).thenReturn(
                new DocumentStorage.StoredObject("S3", "bucket/key", "passport.pdf", "application/pdf", 3, "abc")
        );
        when(repository.save(any(EquipmentDocument.class))).thenAnswer(inv -> {
            EquipmentDocument doc = inv.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID());
            }
            if (doc.getCreatedAt() == null) {
                doc.setCreatedAt(OffsetDateTime.now());
            }
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile("file", "passport.pdf", "application/pdf", "PDF".getBytes());
        EquipmentDocument saved = service.upload(10L, "PASSPORT", file, "u1", "corr-1");

        assertNotNull(saved.getId());
        assertEquals(10L, saved.getEquipmentId());
        assertEquals("PASSPORT", saved.getDocType());
        assertEquals("bucket/key", saved.getStorageLocation());
        verify(outboxService).enqueue(eq("Equipment"), eq(10L), eq("inventory.equipment.document_uploaded"), eq("InventoryEquipmentDocumentUploaded"), any());
    }

    @Test
    void downloadUrl_delegatesToStoragePresign() {
        UUID id = UUID.randomUUID();
        EquipmentDocument doc = new EquipmentDocument();
        doc.setId(id);
        doc.setEquipmentId(10L);
        doc.setStorageType("S3");
        doc.setStorageLocation("bucket/key");
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        when(storage.presignGet(any())).thenReturn(Optional.of(URI.create("http://example")));

        Optional<URI> uri = service.downloadUrl(id);

        assertTrue(uri.isPresent());
        assertEquals("http://example", uri.get().toString());
    }
}

