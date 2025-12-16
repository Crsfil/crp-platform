package com.example.crp.inventory.messaging;

import com.example.crp.inventory.service.InventoryReservationService;
import com.example.crp.inventory.service.InboundReceiptIngestionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InventoryListenersTest {
    private InventoryListeners inventoryListeners;
    @Mock
    private InvalidMessageRouter invalidRouter;
    @Mock
    private InventoryReservationService reservationService;
    @Mock
    private InboundReceiptIngestionService receiptIngestionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inventoryListeners = new InventoryListeners(invalidRouter, reservationService, receiptIngestionService);
    }

    @Test
    void onApproved_routesToService() {
        Events.ProcurementApproved msg = new Events.ProcurementApproved(1L, 4L, 6L);
        when(invalidRouter.isMarkedInvalid("k1")).thenReturn(false);

        inventoryListeners.onApproved(List.of(new ConsumerRecord<>("procurement.approved", 0, 0L, "k1", msg)));

        verify(reservationService).reserveFromProcurementApproval(eq(msg));
        verify(invalidRouter, never()).routeInvalid(eq("procurement.approved.invalid"), any(), any(), any());
    }

    @Test
    void onApproved_skipsPreviouslyInvalidKey() {
        Events.ProcurementApproved msg = new Events.ProcurementApproved(1L, 4L, 6L);
        when(invalidRouter.isMarkedInvalid("k1")).thenReturn(true);

        inventoryListeners.onApproved(List.of(new ConsumerRecord<>("procurement.approved", 0, 0L, "k1", msg)));

        verify(reservationService, never()).reserveFromProcurementApproval(any());
        verify(invalidRouter).routeInvalid(eq("procurement.approved.invalid"), eq("k1"), eq(msg), eq("key previously invalid"));
    }

    @Test
    void onRejected_routesToService() {
        Events.ProcurementRejected msg = new Events.ProcurementRejected(2L, 5L, 7L);
        when(invalidRouter.isMarkedInvalid("k2")).thenReturn(false);

        inventoryListeners.onRejected(List.of(new ConsumerRecord<>("procurement.rejected", 0, 0L, "k2", msg)));

        verify(reservationService).releaseFromProcurementReject(eq(msg));
        verify(invalidRouter, never()).routeInvalid(eq("procurement.rejected.invalid"), any(), any(), any());
    }

    @Test
    void onGoodsAccepted_routesToService() {
        Events.GoodsReceiptAccepted msg = new Events.GoodsReceiptAccepted(
                10L, 20L, 30L, 40L,
                List.of(new Events.GoodsReceiptItem(100L, 200L, "Excavator", BigDecimal.ONE, "PCS", BigDecimal.TEN))
        );
        when(invalidRouter.isMarkedInvalid("k3")).thenReturn(false);

        inventoryListeners.onGoodsAccepted(List.of(new ConsumerRecord<>("procurement.goods_accepted", 0, 0L, "k3", msg)));

        verify(receiptIngestionService).ingest(eq(msg));
        verify(invalidRouter, never()).routeInvalid(eq("procurement.goods_accepted.invalid"), any(), any(), any());
    }
}
