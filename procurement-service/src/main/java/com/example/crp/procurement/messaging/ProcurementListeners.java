package com.example.crp.procurement.messaging;

import com.example.crp.procurement.domain.ProcurementRequest;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcurementListeners {
    private final ProcurementRequestRepository repo;
    private final InvalidMessageRouter invalidRouter;
    public ProcurementListeners(ProcurementRequestRepository repo, InvalidMessageRouter invalidRouter) {
        this.repo = repo; this.invalidRouter = invalidRouter;
    }

    @KafkaListener(topics = "inventory.reserved", groupId = "procurement")
    public void onReserved(List<ConsumerRecord<String, Events.InventoryReserved>> records) {
        for (ConsumerRecord<String, Events.InventoryReserved> rec : records) {
            String key = rec.key();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("inventory.reserved.invalid", key, rec.value(), "key previously invalid");
                continue;
            }
            Events.InventoryReserved msg = rec.value();
            if (msg == null || msg.requestId() == null) {
                invalidRouter.routeInvalid("inventory.reserved.invalid", key, rec.value(), "missing requestId");
                continue;
            }
            try {
                repo.findById(msg.requestId()).ifPresent(pr -> { pr.setStatus("RESERVED"); repo.save(pr); });
            } catch (Exception ex) {
                invalidRouter.routeInvalid("inventory.reserved.invalid", key, rec.value(), ex.getMessage());
            }
        }
    }

    @KafkaListener(topics = "inventory.reserve_failed", groupId = "procurement")
    public void onReserveFailed(List<ConsumerRecord<String, Events.InventoryReserveFailed>> records) {
        for (ConsumerRecord<String, Events.InventoryReserveFailed> rec : records) {
            String key = rec.key();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("inventory.reserve_failed.invalid", key, rec.value(), "key previously invalid");
                continue;
            }
            Events.InventoryReserveFailed msg = rec.value();
            if (msg == null || msg.requestId() == null) {
                invalidRouter.routeInvalid("inventory.reserve_failed.invalid", key, rec.value(), "missing requestId");
                continue;
            }
            try {
                repo.findById(msg.requestId()).ifPresent(pr -> { pr.setStatus("FAILED"); repo.save(pr); });
            } catch (Exception ex) {
                invalidRouter.routeInvalid("inventory.reserve_failed.invalid", key, rec.value(), ex.getMessage());
            }
        }
    }

    @KafkaListener(topics = "inventory.released", groupId = "procurement")
    public void onReleased(List<ConsumerRecord<String, Events.InventoryReleased>> records) {
        for (ConsumerRecord<String, Events.InventoryReleased> rec : records) {
            String key = rec.key();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("inventory.released.invalid", key, rec.value(), "key previously invalid");
                continue;
            }
            Events.InventoryReleased msg = rec.value();
            if (msg == null || msg.requestId() == null) {
                invalidRouter.routeInvalid("inventory.released.invalid", key, rec.value(), "missing requestId");
                continue;
            }
            try {
                repo.findById(msg.requestId()).ifPresent(pr -> { pr.setStatus("REJECTED"); repo.save(pr); });
            } catch (Exception ex) {
                invalidRouter.routeInvalid("inventory.released.invalid", key, rec.value(), ex.getMessage());
            }
        }
    }
}
