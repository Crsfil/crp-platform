package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.EquipmentInspection;
import com.example.crp.inventory.domain.EquipmentInspectionFinding;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentInspectionDocumentLinkRepository;
import com.example.crp.inventory.repo.EquipmentInspectionFindingRepository;
import com.example.crp.inventory.repo.EquipmentInspectionRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.LocationRepository;
import com.example.crp.inventory.security.LocationAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EquipmentInspectionServiceTest {

    private EquipmentRepository equipmentRepository;
    private LocationRepository locationRepository;
    private EquipmentInspectionRepository inspectionRepository;
    private EquipmentInspectionFindingRepository findingRepository;
    private EquipmentInspectionDocumentLinkRepository documentLinkRepository;
    private EquipmentDocumentService documentService;
    private EquipmentLifecycleService lifecycleService;
    private LocationAccessPolicy locationAccessPolicy;
    private OutboxService outboxService;

    private EquipmentInspectionService service;

    @BeforeEach
    void setUp() {
        equipmentRepository = mock(EquipmentRepository.class);
        locationRepository = mock(LocationRepository.class);
        inspectionRepository = mock(EquipmentInspectionRepository.class);
        findingRepository = mock(EquipmentInspectionFindingRepository.class);
        documentLinkRepository = mock(EquipmentInspectionDocumentLinkRepository.class);
        documentService = mock(EquipmentDocumentService.class);
        lifecycleService = mock(EquipmentLifecycleService.class);
        locationAccessPolicy = mock(LocationAccessPolicy.class);
        outboxService = mock(OutboxService.class);
        service = new EquipmentInspectionService(equipmentRepository, locationRepository, inspectionRepository, findingRepository,
                documentLinkRepository, documentService, lifecycleService, locationAccessPolicy, outboxService);
    }

    @Test
    void create_createsDraft_andEnqueuesEvent() {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(new com.example.crp.inventory.domain.Equipment()));
        when(inspectionRepository.existsByEquipmentIdAndStatusIn(eq(10L), anyCollection())).thenReturn(false);
        when(inspectionRepository.save(any())).thenAnswer(inv -> {
            EquipmentInspection i = inv.getArgument(0);
            try {
                java.lang.reflect.Field f = EquipmentInspection.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(i, 1L);
            } catch (Exception ignored) {}
            return i;
        });

        Authentication auth = mock(Authentication.class);
        EquipmentInspection i = service.create(10L, "RETURN", null, "s", "u1", "c", auth);

        assertEquals("DRAFT", i.getStatus());
        assertEquals("RETURN", i.getType());
        verify(outboxService).enqueue(eq("EquipmentInspection"), eq(1L), eq("inventory.equipment.inspection_created"),
                eq("InventoryEquipmentInspectionCreated"), any());
    }

    @Test
    void addFinding_requiresDraftOrSubmitted() {
        EquipmentInspection i = new EquipmentInspection();
        i.setEquipmentId(10L);
        i.setStatus("COMPLETED");
        when(inspectionRepository.findById(1L)).thenReturn(Optional.of(i));
        assertThrows(IllegalStateException.class, () -> service.addFinding(1L, null, "MINOR", null, null));
    }

    @Test
    void complete_setsEquipmentStatus_whenDamaged() {
        EquipmentInspection i = new EquipmentInspection();
        i.setEquipmentId(10L);
        i.setStatus("APPROVED");
        try {
            java.lang.reflect.Field f = EquipmentInspection.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(i, 5L);
        } catch (Exception ignored) {}
        when(inspectionRepository.findById(5L)).thenReturn(Optional.of(i));
        when(inspectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Authentication auth = mock(Authentication.class);
        service.complete(5L, "DAMAGED", "REPAIR", null, "u1", "corr", auth);

        verify(lifecycleService).changeStatus(eq(10L), eq("DAMAGED"), eq("inspection"), eq("u1"), eq("corr"), eq(auth));
        verify(outboxService).enqueue(eq("EquipmentInspection"), eq(5L), eq("inventory.equipment.inspection_completed"),
                eq("InventoryEquipmentInspectionCompleted"), any());
    }
}

