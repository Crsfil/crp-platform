package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentRepairOrder;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentInspectionRepository;
import com.example.crp.inventory.repo.EquipmentRepairDocumentLinkRepository;
import com.example.crp.inventory.repo.EquipmentRepairLineRepository;
import com.example.crp.inventory.repo.EquipmentRepairOrderRepository;
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

class EquipmentRepairServiceTest {

    private EquipmentRepository equipmentRepository;
    private EquipmentInspectionRepository inspectionRepository;
    private LocationRepository locationRepository;
    private EquipmentRepairOrderRepository repairRepository;
    private EquipmentRepairDocumentLinkRepository documentLinkRepository;
    private EquipmentRepairLineRepository lineRepository;
    private EquipmentDocumentService documentService;
    private EquipmentLifecycleService lifecycleService;
    private EquipmentInspectionService inspectionService;
    private LocationAccessPolicy locationAccessPolicy;
    private OutboxService outboxService;

    private EquipmentRepairService service;

    @BeforeEach
    void setUp() {
        equipmentRepository = mock(EquipmentRepository.class);
        inspectionRepository = mock(EquipmentInspectionRepository.class);
        locationRepository = mock(LocationRepository.class);
        repairRepository = mock(EquipmentRepairOrderRepository.class);
        documentLinkRepository = mock(EquipmentRepairDocumentLinkRepository.class);
        lineRepository = mock(EquipmentRepairLineRepository.class);
        documentService = mock(EquipmentDocumentService.class);
        lifecycleService = mock(EquipmentLifecycleService.class);
        inspectionService = mock(EquipmentInspectionService.class);
        locationAccessPolicy = mock(LocationAccessPolicy.class);
        outboxService = mock(OutboxService.class);
        service = new EquipmentRepairService(equipmentRepository, inspectionRepository, locationRepository, repairRepository,
                documentLinkRepository, lineRepository, documentService, lifecycleService, inspectionService, locationAccessPolicy, outboxService);
    }

    @Test
    void create_createsDraft_andEmitsEvent() {
        Equipment eq = new Equipment();
        eq.setId(10L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(eq));
        when(repairRepository.existsByEquipmentIdAndStatusIn(eq(10L), anyCollection())).thenReturn(false);
        when(repairRepository.save(any())).thenAnswer(inv -> {
            EquipmentRepairOrder r = inv.getArgument(0);
            try {
                java.lang.reflect.Field f = EquipmentRepairOrder.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(r, 1L);
            } catch (Exception ignored) {}
            return r;
        });

        Authentication auth = mock(Authentication.class);
        EquipmentRepairOrder r = service.create(10L, null, null, null, null, null, null, null, "u1", "c", auth);

        assertEquals("DRAFT", r.getStatus());
        verify(outboxService).enqueue(eq("EquipmentRepair"), eq(1L), eq("inventory.equipment.repair_created"),
                eq("InventoryEquipmentRepairCreated"), any());
    }

    @Test
    void complete_changesEquipmentStatus() {
        EquipmentRepairOrder r = new EquipmentRepairOrder();
        r.setEquipmentId(10L);
        r.setStatus("IN_PROGRESS");
        try {
            java.lang.reflect.Field f = EquipmentRepairOrder.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, 9L);
        } catch (Exception ignored) {}
        when(repairRepository.findById(9L)).thenReturn(Optional.of(r));
        when(repairRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(new Equipment()));
        when(lineRepository.sumTotalCostByRepairId(9L)).thenReturn(java.math.BigDecimal.ZERO);

        Authentication auth = mock(Authentication.class);
        EquipmentRepairOrder saved = service.complete(9L, null, null, true, false, null, "u1", "corr", auth);
        assertEquals("COMPLETED", saved.getStatus());
        verify(lifecycleService).changeStatus(eq(10L), eq("AVAILABLE"), eq("repair_completed"), eq("u1"), eq("corr"), eq(auth));
        verify(outboxService).enqueue(eq("EquipmentRepair"), eq(9L), eq("inventory.equipment.repair_completed"),
                eq("InventoryEquipmentRepairCompleted"), any());
    }
}
