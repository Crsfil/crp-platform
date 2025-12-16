package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.EquipmentDisposition;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentDispositionRepository;
import com.example.crp.inventory.repo.EquipmentLeaseRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EquipmentDispositionServiceTest {

    private EquipmentRepository equipmentRepository;
    private EquipmentLeaseRepository leaseRepository;
    private EquipmentDispositionRepository dispositionRepository;
    private EquipmentLifecycleService lifecycleService;
    private OutboxService outboxService;

    private EquipmentDispositionService service;

    @BeforeEach
    void setUp() {
        equipmentRepository = mock(EquipmentRepository.class);
        leaseRepository = mock(EquipmentLeaseRepository.class);
        dispositionRepository = mock(EquipmentDispositionRepository.class);
        lifecycleService = mock(EquipmentLifecycleService.class);
        outboxService = mock(OutboxService.class);
        service = new EquipmentDispositionService(equipmentRepository, leaseRepository, dispositionRepository, lifecycleService, outboxService);
    }

    @Test
    void create_createsDraft_andEnqueuesEvent() {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(new com.example.crp.inventory.domain.Equipment()));
        when(dispositionRepository.existsByEquipmentIdAndStatusIn(eq(10L), anyCollection())).thenReturn(false);
        when(leaseRepository.existsByEquipmentIdAndStatus(10L, "ACTIVE")).thenReturn(false);
        when(dispositionRepository.save(any())).thenAnswer(inv -> {
            EquipmentDisposition d = inv.getArgument(0);
            try {
                java.lang.reflect.Field f = EquipmentDisposition.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(d, 55L);
            } catch (Exception ignored) {}
            return d;
        });

        EquipmentDisposition d = service.create(10L, "sale", new BigDecimal("100.00"), "RUB", "Buyer", "7700000000",
                2L, "note", "u1", "corr");

        assertEquals("DRAFT", d.getStatus());
        assertEquals("SALE", d.getType());
        verify(outboxService).enqueue(eq("EquipmentDisposition"), eq(55L), eq("inventory.equipment.disposition_created"),
                eq("InventoryEquipmentDispositionCreated"), any());
    }

    @Test
    void approve_requiresDraft() {
        EquipmentDisposition d = new EquipmentDisposition();
        d.setEquipmentId(10L);
        d.setType("SALE");
        d.setStatus("APPROVED");
        when(dispositionRepository.findById(1L)).thenReturn(Optional.of(d));
        assertThrows(IllegalStateException.class, () -> service.approve(1L, "u1", "c"));
    }

    @Test
    void complete_changesEquipmentStatus_andEnqueuesEvent() {
        EquipmentDisposition d = new EquipmentDisposition();
        d.setEquipmentId(10L);
        d.setType("DISPOSAL");
        d.setStatus("APPROVED");
        d.setLocationId(7L);
        try {
            java.lang.reflect.Field f = EquipmentDisposition.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(d, 99L);
        } catch (Exception ignored) {}
        when(dispositionRepository.findById(99L)).thenReturn(Optional.of(d));
        when(leaseRepository.existsByEquipmentIdAndStatus(10L, "ACTIVE")).thenReturn(false);
        when(dispositionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lifecycleService.transfer(anyLong(), anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(new com.example.crp.inventory.domain.Equipment());
        when(lifecycleService.changeStatus(anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(new com.example.crp.inventory.domain.Equipment());

        Authentication auth = mock(Authentication.class);
        EquipmentDisposition saved = service.complete(99L, new BigDecimal("1.23"), null, "u2", "corr2", auth);

        assertEquals("COMPLETED", saved.getStatus());
        assertNotNull(saved.getPerformedAt());
        verify(lifecycleService).transfer(eq(10L), eq(7L), isNull(), eq("disposition"), eq("u2"), eq("corr2"), eq(auth));
        verify(lifecycleService).changeStatus(eq(10L), eq("DISPOSED"), eq("disposition"), eq("u2"), eq("corr2"), eq(auth));
        verify(outboxService).enqueue(eq("EquipmentDisposition"), eq(99L), eq("inventory.equipment.disposition_completed"),
                eq("InventoryEquipmentDispositionCompleted"), any());
    }

    @Test
    void sale_workflow_contract_invoice_paid() {
        EquipmentDisposition d = new EquipmentDisposition();
        d.setEquipmentId(10L);
        d.setType("SALE");
        d.setStatus("APPROVED");
        try {
            java.lang.reflect.Field f = EquipmentDisposition.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(d, 77L);
        } catch (Exception ignored) {}
        when(dispositionRepository.findById(77L)).thenReturn(Optional.of(d));
        when(dispositionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EquipmentDisposition contracted = service.contractSale(77L, "direct", "LOT-1", "C-1", "u1", "corr1");
        assertEquals("CONTRACTED", contracted.getStatus());
        assertEquals("direct", contracted.getSaleMethod());
        assertEquals("LOT-1", contracted.getLotNumber());
        assertEquals("C-1", contracted.getContractNumber());
        verify(outboxService).enqueue(eq("EquipmentDisposition"), eq(77L), eq("inventory.equipment.sale_contracted"),
                eq("InventoryEquipmentSaleContracted"), any());

        EquipmentDisposition invoiced = service.invoiceSale(77L, "INV-1", "u2", "corr2");
        assertEquals("INVOICED", invoiced.getStatus());
        assertEquals("INV-1", invoiced.getInvoiceNumber());
        verify(outboxService).enqueue(eq("EquipmentDisposition"), eq(77L), eq("inventory.equipment.sale_invoiced"),
                eq("InventoryEquipmentSaleInvoiced"), any());

        OffsetDateTime paidAt = OffsetDateTime.now();
        EquipmentDisposition paid = service.markPaid(77L, paidAt, "u3", "corr3");
        assertEquals("PAID", paid.getStatus());
        assertNotNull(paid.getPaidAt());
        verify(outboxService).enqueue(eq("EquipmentDisposition"), eq(77L), eq("inventory.equipment.sale_paid"),
                eq("InventoryEquipmentSalePaid"), any());
    }
}
