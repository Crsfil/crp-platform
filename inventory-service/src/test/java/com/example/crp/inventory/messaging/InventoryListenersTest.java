package com.example.crp.inventory.messaging;

import com.example.crp.inventory.service.InventoryReservationService;
import com.example.crp.inventory.service.InboundReceiptIngestionService;
import com.example.crp.inventory.service.EquipmentRepossessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Mock
    private EquipmentRepossessionService repossessionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        inventoryListeners = new InventoryListeners(invalidRouter, reservationService, receiptIngestionService, repossessionService, objectMapper);
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

    @Test
    void onServiceCompleted_convertsMapPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceOrderId", 100L);
        payload.put("serviceType", "SERVICE_STORAGE");
        payload.put("equipmentId", 55L);
        payload.put("locationId", 12L);
        payload.put("supplierId", 3L);
        payload.put("actualCost", BigDecimal.TEN);
        payload.put("actDocumentId", null);
        payload.put("completedAt", OffsetDateTime.parse("2025-01-01T10:00:00Z").toString());
        when(invalidRouter.isMarkedInvalid("k4")).thenReturn(false);

        @SuppressWarnings({"rawtypes","unchecked"})
        ConsumerRecord rec = new ConsumerRecord("procurement.service_completed", 0, 0L, "k4", payload);
        inventoryListeners.onServiceCompleted(List.of((ConsumerRecord<String, Events.ProcurementServiceCompleted>) rec));

        verify(repossessionService).handleServiceCompleted(argThat(m ->
                m != null && m.serviceOrderId().equals(100L) && m.equipmentId().equals(55L)
        ));
    }

    @Test
    void onServiceCreated_convertsMapPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceOrderId", 200L);
        payload.put("serviceType", "SERVICE_STORAGE");
        payload.put("equipmentId", 77L);
        payload.put("locationId", 18L);
        payload.put("supplierId", 5L);
        when(invalidRouter.isMarkedInvalid("k5")).thenReturn(false);

        @SuppressWarnings({"rawtypes","unchecked"})
        ConsumerRecord rec = new ConsumerRecord("procurement.service_created", 0, 0L, "k5", payload);
        inventoryListeners.onServiceCreated(List.of((ConsumerRecord<String, Events.ProcurementServiceCreated>) rec));

        verify(repossessionService).handleServiceCreated(argThat(m ->
                m != null && m.serviceOrderId().equals(200L) && m.equipmentId().equals(77L)
        ));
    }
}
