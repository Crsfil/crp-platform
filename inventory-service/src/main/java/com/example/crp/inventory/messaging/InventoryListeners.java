package com.example.crp.inventory.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.crp.inventory.service.InventoryReservationService;

import java.util.List;

@Component
public class InventoryListeners {
    private final InvalidMessageRouter invalidRouter;
    private final InventoryReservationService reservationService;
    private final com.example.crp.inventory.service.InboundReceiptIngestionService receiptIngestionService;
    private final com.example.crp.inventory.service.EquipmentRepossessionService repossessionService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public InventoryListeners(InvalidMessageRouter invalidRouter,
                              InventoryReservationService reservationService,
                              com.example.crp.inventory.service.InboundReceiptIngestionService receiptIngestionService,
                              com.example.crp.inventory.service.EquipmentRepossessionService repossessionService,
                              com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.invalidRouter = invalidRouter;
        this.reservationService = reservationService;
        this.receiptIngestionService = receiptIngestionService;
        this.repossessionService = repossessionService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "procurement.approved", groupId = "inventory")
    public void onApproved(List<ConsumerRecord<String, Events.ProcurementApproved>> records) {
        for (ConsumerRecord<String, Events.ProcurementApproved> rec : records) {
            String key = rec.key();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("procurement.approved.invalid", key, rec.value(), "key previously invalid");
                continue;
            }
            Events.ProcurementApproved msg;
            try {
                msg = convertValue(rec.value(), Events.ProcurementApproved.class);
            } catch (IllegalArgumentException ex) {
                invalidRouter.routeInvalid("procurement.approved.invalid", key, rec.value(), "invalid payload");
                continue;
            }
            if (msg == null || msg.requestId() == null || msg.equipmentId() == null) {
                invalidRouter.routeInvalid("procurement.approved.invalid", key, rec.value(), "missing ids");
                continue;
            }
            try {
                reservationService.reserveFromProcurementApproval(msg);
            } catch (Exception ex) {
                invalidRouter.routeInvalid("procurement.approved.invalid", key, rec.value(), ex.getMessage());
            }
        }
    }

    @KafkaListener(topics = "procurement.rejected", groupId = "inventory")
    public void onRejected(List<ConsumerRecord<String, Events.ProcurementRejected>> records) {
        for (ConsumerRecord<String, Events.ProcurementRejected> rec : records) {
            String key = rec.key();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("procurement.rejected.invalid", key, rec.value(), "key previously invalid");
                continue;
            }
            Events.ProcurementRejected msg;
            try {
                msg = convertValue(rec.value(), Events.ProcurementRejected.class);
            } catch (IllegalArgumentException ex) {
                invalidRouter.routeInvalid("procurement.rejected.invalid", key, rec.value(), "invalid payload");
                continue;
            }
            if (msg == null || msg.requestId() == null || msg.equipmentId() == null) {
                invalidRouter.routeInvalid("procurement.rejected.invalid", key, rec.value(), "missing ids");
                continue;
            }
            try {
                reservationService.releaseFromProcurementReject(msg);
            } catch (Exception ex) {
                invalidRouter.routeInvalid("procurement.rejected.invalid", key, rec.value(), ex.getMessage());
            }
        }
    }

    @KafkaListener(topics = "procurement.goods_accepted", groupId = "inventory")
    public void onGoodsAccepted(List<ConsumerRecord<String, Events.GoodsReceiptAccepted>> records) {
        for (ConsumerRecord<String, Events.GoodsReceiptAccepted> rec : records) {
            String key = rec.key();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("procurement.goods_accepted.invalid", key, rec.value(), "key previously invalid");
                continue;
            }
            Events.GoodsReceiptAccepted msg;
            try {
                msg = convertValue(rec.value(), Events.GoodsReceiptAccepted.class);
            } catch (IllegalArgumentException ex) {
                invalidRouter.routeInvalid("procurement.goods_accepted.invalid", key, rec.value(), "invalid payload");
                continue;
            }
            if (msg == null || msg.receiptId() == null) {
                invalidRouter.routeInvalid("procurement.goods_accepted.invalid", key, rec.value(), "missing receiptId");
                continue;
            }
            try {
                receiptIngestionService.ingest(msg);
            } catch (Exception ex) {
                invalidRouter.routeInvalid("procurement.goods_accepted.invalid", key, rec.value(), ex.getMessage());
            }
        }
    }

    @KafkaListener(topics = "procurement.service_completed", groupId = "inventory")
    public void onServiceCompleted(List<ConsumerRecord<String, Events.ProcurementServiceCompleted>> records) {
        for (ConsumerRecord<String, Events.ProcurementServiceCompleted> rec : records) {
            String key = rec.key();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("procurement.service_completed.invalid", key, rec.value(), "key previously invalid");
                continue;
            }
            Events.ProcurementServiceCompleted msg;
            try {
                msg = convertValue(rec.value(), Events.ProcurementServiceCompleted.class);
            } catch (IllegalArgumentException ex) {
                invalidRouter.routeInvalid("procurement.service_completed.invalid", key, rec.value(), "invalid payload");
                continue;
            }
            if (msg == null || msg.equipmentId() == null) {
                invalidRouter.routeInvalid("procurement.service_completed.invalid", key, rec.value(), "missing equipmentId");
                continue;
            }
            try {
                repossessionService.handleServiceCompleted(msg);
            } catch (Exception ex) {
                invalidRouter.routeInvalid("procurement.service_completed.invalid", key, rec.value(), ex.getMessage());
            }
        }
    }

    @KafkaListener(topics = "procurement.service_created", groupId = "inventory")
    public void onServiceCreated(List<ConsumerRecord<String, Events.ProcurementServiceCreated>> records) {
        for (ConsumerRecord<String, Events.ProcurementServiceCreated> rec : records) {
            String key = rec.key();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("procurement.service_created.invalid", key, rec.value(), "key previously invalid");
                continue;
            }
            Events.ProcurementServiceCreated msg;
            try {
                msg = convertValue(rec.value(), Events.ProcurementServiceCreated.class);
            } catch (IllegalArgumentException ex) {
                invalidRouter.routeInvalid("procurement.service_created.invalid", key, rec.value(), "invalid payload");
                continue;
            }
            if (msg == null || msg.equipmentId() == null || msg.serviceOrderId() == null) {
                invalidRouter.routeInvalid("procurement.service_created.invalid", key, rec.value(), "missing ids");
                continue;
            }
            try {
                repossessionService.handleServiceCreated(msg);
            } catch (Exception ex) {
                invalidRouter.routeInvalid("procurement.service_created.invalid", key, rec.value(), ex.getMessage());
            }
        }
    }

    private <T> T convertValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return objectMapper.convertValue(value, type);
    }
}
