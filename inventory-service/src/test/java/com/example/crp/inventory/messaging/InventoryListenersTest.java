package com.example.crp.inventory.messaging;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InventoryListenersTest {
    private InventoryListeners inventoryListeners;
    @Mock
    private EquipmentRepository repo;
    @Mock
    private KafkaTemplate<String, Object> template;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inventoryListeners = new InventoryListeners(repo, template);
    }

    @Test
    void onApproved() {
        Events.ProcurementApproved msg = new Events.ProcurementApproved(1L,4L,6L);
        Equipment equipmentFind = new Equipment();
        equipmentFind.setStatus("AVAILABLE");

        when(repo.findById(4L)).thenReturn(Optional.of(equipmentFind));

        inventoryListeners.onApproved(msg);

        assertEquals("RESERVED", equipmentFind.getStatus());
        verify(repo).save(equipmentFind);
        verify(template).send(eq("inventory.reserved"), any(Events.InventoryReserved.class));
        verify(template, never()).send(eq("inventory.reserve_failed"), any());

    }

    @Test
    void onApprovedEquipmentNotFound() {
        Events.ProcurementApproved msg = new Events.ProcurementApproved(10L, 99L, 6L);
        when(repo.findById(99L)).thenReturn(Optional.empty());

        inventoryListeners.onApproved(msg);

        verify(repo, never()).save(any());
        verify(template).send(eq("inventory.reserve_failed"), any(Events.InventoryReserveFailed.class));
        verify(template, never()).send(eq("inventory.reserved"), any());
    }

    @Test
    void onApprovedEquipmentNotAvailable() {
        Events.ProcurementApproved msg = new Events.ProcurementApproved(11L, 100L, 6L);
        Equipment equipment = new Equipment();
        equipment.setStatus("RESERVED");
        when(repo.findById(100L)).thenReturn(Optional.of(equipment));

        inventoryListeners.onApproved(msg);

        verify(repo, never()).save(any());
        verify(template).send(eq("inventory.reserve_failed"), any(Events.InventoryReserveFailed.class));
        verify(template, never()).send(eq("inventory.reserved"), any());

    }

    @Test
    void onRejected() {
        Events.ProcurementRejected msg = new Events.ProcurementRejected(2L, 5L, 7L);
        Equipment equipmentToFind = new Equipment();
        equipmentToFind.setStatus("RESERVED");

        when(repo.findById(5L)).thenReturn(Optional.of(equipmentToFind));

        inventoryListeners.onRejected(msg);
        assertEquals("AVAILABLE", equipmentToFind.getStatus());
        verify(repo).save(equipmentToFind);
        verify(template).send(eq("inventory.released"), any(Events.InventoryReleased.class));
    }

    @Test
    void onRejectedEqEmpty() {
        Events.ProcurementRejected msg = new Events.ProcurementRejected(2L, 5L, 7L);

        when(repo.findById(5L)).thenReturn(Optional.empty());

        inventoryListeners.onRejected(msg);
        verify(repo, never()).save(any());
        verify(template).send(eq("inventory.released"), any(Events.InventoryReleased.class));
    }
}
