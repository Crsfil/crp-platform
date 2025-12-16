package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentMovement;
import com.example.crp.inventory.domain.EquipmentStatusHistory;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentMovementRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.EquipmentStatusHistoryRepository;
import com.example.crp.inventory.repo.LocationRepository;
import com.example.crp.inventory.security.LocationAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EquipmentLifecycleServiceTest {

    private EquipmentRepository equipmentRepository;
    private LocationRepository locationRepository;
    private EquipmentMovementRepository movementRepository;
    private EquipmentStatusHistoryRepository statusHistoryRepository;
    private OutboxService outboxService;
    private LocationAccessPolicy locationAccessPolicy;

    private EquipmentLifecycleService service;

    @BeforeEach
    void setUp() {
        equipmentRepository = mock(EquipmentRepository.class);
        locationRepository = mock(LocationRepository.class);
        movementRepository = mock(EquipmentMovementRepository.class);
        statusHistoryRepository = mock(EquipmentStatusHistoryRepository.class);
        outboxService = mock(OutboxService.class);
        locationAccessPolicy = mock(LocationAccessPolicy.class);
        service = new EquipmentLifecycleService(equipmentRepository, locationRepository, movementRepository, statusHistoryRepository, outboxService, locationAccessPolicy);
    }

    @Test
    void transfer_updatesEquipment_writesMovement_andEnqueuesOutbox() {
        Equipment e = new Equipment();
        e.setId(10L);
        e.setStatus("AVAILABLE");
        e.setLocationId(1L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e));
        com.example.crp.inventory.domain.Location to = new com.example.crp.inventory.domain.Location();
        to.setStatus("ACTIVE");
        when(locationRepository.findById(2L)).thenReturn(Optional.of(to));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(new com.example.crp.inventory.domain.Location()));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any(EquipmentMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication auth = mock(Authentication.class);
        Equipment saved = service.transfer(10L, 2L, "boss", "move", "user1", "corr-1", auth);

        assertEquals(2L, saved.getLocationId());
        assertEquals("boss", saved.getResponsibleUsername());

        ArgumentCaptor<EquipmentMovement> movementCaptor = ArgumentCaptor.forClass(EquipmentMovement.class);
        verify(movementRepository).save(movementCaptor.capture());
        assertEquals(10L, movementCaptor.getValue().getEquipmentId());
        assertEquals(1L, movementCaptor.getValue().getFromLocationId());
        assertEquals(2L, movementCaptor.getValue().getToLocationId());
        assertEquals("user1", movementCaptor.getValue().getMovedBy());
        assertEquals("corr-1", movementCaptor.getValue().getCorrelationId());

        verify(outboxService).enqueue(eq("Equipment"), eq(10L), eq("inventory.equipment.transferred"), eq("InventoryEquipmentTransferred"), any());
    }

    @Test
    void changeStatus_noopWhenSameStatus() {
        Equipment e = new Equipment();
        e.setId(10L);
        e.setStatus("IN_STORAGE");
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e));

        Equipment saved = service.changeStatus(10L, "in_storage", "same", "u1", "corr-2", null);

        assertSame(e, saved);
        verify(equipmentRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(outboxService, never()).enqueue(anyString(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void changeStatus_writesHistory_andEnqueuesOutbox() {
        Equipment e = new Equipment();
        e.setId(10L);
        e.setStatus("AVAILABLE");
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any(EquipmentStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipment saved = service.changeStatus(10L, "RESERVED", "reserved", "u1", "corr-3", null);

        assertEquals("RESERVED", saved.getStatus());

        ArgumentCaptor<EquipmentStatusHistory> historyCaptor = ArgumentCaptor.forClass(EquipmentStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertEquals(10L, historyCaptor.getValue().getEquipmentId());
        assertEquals("AVAILABLE", historyCaptor.getValue().getFromStatus());
        assertEquals("RESERVED", historyCaptor.getValue().getToStatus());
        assertEquals("u1", historyCaptor.getValue().getChangedBy());
        assertEquals("corr-3", historyCaptor.getValue().getCorrelationId());

        verify(outboxService).enqueue(eq("Equipment"), eq(10L), eq("inventory.equipment.status_changed"), eq("InventoryEquipmentStatusChanged"), any());
    }

    @Test
    void transfer_rejectsUnknownLocation() {
        Equipment e = new Equipment();
        e.setId(10L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e));
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.transfer(10L, 99L, null, null, "u1", "c1", mock(Authentication.class)));
        assertTrue(ex.getMessage().toLowerCase().contains("location"));
    }
}
