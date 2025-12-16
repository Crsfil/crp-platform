package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentLease;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentLeaseRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EquipmentLeaseServiceTest {

    private EquipmentRepository equipmentRepository;
    private EquipmentLeaseRepository leaseRepository;
    private EquipmentLifecycleService lifecycleService;
    private OutboxService outboxService;
    private EquipmentLeaseService service;

    @BeforeEach
    void setUp() {
        equipmentRepository = mock(EquipmentRepository.class);
        leaseRepository = mock(EquipmentLeaseRepository.class);
        lifecycleService = mock(EquipmentLifecycleService.class);
        outboxService = mock(OutboxService.class);
        service = new EquipmentLeaseService(equipmentRepository, leaseRepository, lifecycleService, outboxService);
    }

    @Test
    void start_createsLease_andEmitsEvent() {
        Equipment eq = new Equipment();
        eq.setId(10L);
        eq.setLocationId(1L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(eq));
        when(leaseRepository.existsByEquipmentIdAndStatus(10L, "ACTIVE")).thenReturn(false);
        when(leaseRepository.save(any())).thenAnswer(inv -> {
            EquipmentLease l = inv.getArgument(0);
            try {
                java.lang.reflect.Field f = EquipmentLease.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(l, 99L);
            } catch (Exception ignored) {}
            return l;
        });
        when(lifecycleService.transfer(anyLong(), anyLong(), any(), any(), any(), any(), any())).thenReturn(eq);
        when(lifecycleService.changeStatus(anyLong(), anyString(), any(), any(), any(), any())).thenReturn(eq);

        Authentication auth = mock(Authentication.class);
        EquipmentLease lease = service.start(10L, 2L, 7L, 8L, null, " note ", "u1", "corr", auth);

        assertEquals("ACTIVE", lease.getStatus());
        assertEquals(10L, lease.getEquipmentId());
        assertEquals(7L, lease.getAgreementId());
        assertEquals(8L, lease.getCustomerId());
        assertEquals(1L, lease.getIssuedFromLocationId());
        assertEquals(2L, lease.getIssuedToLocationId());
        assertEquals("note", lease.getNote());

        verify(lifecycleService).transfer(eq(10L), eq(2L), isNull(), eq("lease_out"), eq("u1"), eq("corr"), eq(auth));
        verify(lifecycleService).changeStatus(eq(10L), eq("LEASED"), eq("lease_out"), eq("u1"), eq("corr"), eq(auth));
        verify(outboxService).enqueue(eq("EquipmentLease"), eq(99L), eq("inventory.equipment.lease_started"), eq("InventoryEquipmentLeaseStarted"), any());
    }

    @Test
    void returnFromLease_updatesLease_andEmitsEvent() {
        Equipment eq = new Equipment();
        eq.setId(10L);
        eq.setStatus("LEASED");
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(eq));

        EquipmentLease active = new EquipmentLease();
        active.setEquipmentId(10L);
        active.setStatus("ACTIVE");
        try {
            java.lang.reflect.Field f = EquipmentLease.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(active, 77L);
        } catch (Exception ignored) {}
        when(leaseRepository.findFirstByEquipmentIdAndStatusOrderByCreatedAtDesc(10L, "ACTIVE")).thenReturn(Optional.of(active));
        when(leaseRepository.save(any())).thenAnswer(inv -> {
            EquipmentLease l = inv.getArgument(0);
            try {
                java.lang.reflect.Field f = EquipmentLease.class.getDeclaredField("id");
                f.setAccessible(true);
                if (f.get(l) == null) {
                    f.set(l, 77L);
                }
            } catch (Exception ignored) {}
            return l;
        });
        when(lifecycleService.transfer(anyLong(), anyLong(), any(), any(), any(), any(), any())).thenReturn(eq);
        when(lifecycleService.changeStatus(anyLong(), anyString(), any(), any(), any(), any())).thenReturn(eq);

        Authentication auth = mock(Authentication.class);
        EquipmentLease saved = service.returnFromLease(10L, 5L, null, null, "u2", "corr2", auth);

        assertEquals("CLOSED", saved.getStatus());
        assertNotNull(saved.getReturnedAt());
        assertNotNull(saved.getEndAt());

        verify(lifecycleService).transfer(eq(10L), eq(5L), isNull(), eq("lease_return"), eq("u2"), eq("corr2"), eq(auth));
        verify(lifecycleService).changeStatus(eq(10L), eq("RETURNED"), eq("lease_return"), eq("u2"), eq("corr2"), eq(auth));
        verify(lifecycleService).changeStatus(eq(10L), eq("IN_STORAGE"), eq("post_return"), eq("u2"), eq("corr2"), eq(auth));
        verify(outboxService).enqueue(eq("EquipmentLease"), anyLong(), eq("inventory.equipment.lease_returned"), eq("InventoryEquipmentLeaseReturned"), any());
    }

    @Test
    void repossess_marksDefaulted_andEmitsEvent() {
        EquipmentLease active = new EquipmentLease();
        active.setEquipmentId(10L);
        active.setStatus("ACTIVE");
        try {
            java.lang.reflect.Field f = EquipmentLease.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(active, 88L);
        } catch (Exception ignored) {}
        when(leaseRepository.findFirstByEquipmentIdAndStatusOrderByCreatedAtDesc(10L, "ACTIVE")).thenReturn(Optional.of(active));
        when(leaseRepository.save(any())).thenAnswer(inv -> {
            EquipmentLease l = inv.getArgument(0);
            try {
                java.lang.reflect.Field f = EquipmentLease.class.getDeclaredField("id");
                f.setAccessible(true);
                if (f.get(l) == null) {
                    f.set(l, 88L);
                }
            } catch (Exception ignored) {}
            return l;
        });
        Equipment eq = new Equipment();
        eq.setId(10L);
        when(lifecycleService.transfer(anyLong(), anyLong(), any(), any(), any(), any(), any())).thenReturn(eq);
        when(lifecycleService.changeStatus(anyLong(), anyString(), any(), any(), any(), any())).thenReturn(eq);

        Authentication auth = mock(Authentication.class);
        EquipmentLease saved = service.repossess(10L, 9L, "x", "u3", "corr3", auth);

        assertEquals("DEFAULTED", saved.getStatus());
        assertNotNull(saved.getRepossessedAt());
        verify(outboxService).enqueue(eq("EquipmentLease"), anyLong(), eq("inventory.equipment.lease_repossessed"), eq("InventoryEquipmentLeaseRepossessed"), any());
    }
}
