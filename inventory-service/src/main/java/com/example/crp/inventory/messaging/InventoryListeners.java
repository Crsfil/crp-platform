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

    public InventoryListeners(InvalidMessageRouter invalidRouter,
                              InventoryReservationService reservationService) {
        this.invalidRouter = invalidRouter;
        this.reservationService = reservationService;
    }

    @KafkaListener(topics = "procurement.approved", groupId = "inventory")
    public void onApproved(List<ConsumerRecord<String, Events.ProcurementApproved>> records) {
        for (ConsumerRecord<String, Events.ProcurementApproved> rec : records) {
            String key = rec.key();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("procurement.approved.invalid", key, rec.value(), "key previously invalid");
                continue;
            }
            Events.ProcurementApproved msg = rec.value();
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
            Events.ProcurementRejected msg = rec.value();
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
}
