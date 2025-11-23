package com.example.crp.inventory.messaging;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class InventoryListeners {
    private final EquipmentRepository repo;
    private final KafkaTemplate<String, Object> template;
    private final InvalidMessageRouter invalidRouter;
    public InventoryListeners(EquipmentRepository repo, KafkaTemplate<String, Object> template, InvalidMessageRouter invalidRouter) {
        this.repo = repo; this.template = template; this.invalidRouter = invalidRouter;
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
                Optional<Equipment> eOpt = repo.findById(msg.equipmentId());
                if (eOpt.isEmpty()) {
                    template.send("inventory.reserve_failed", key, new Events.InventoryReserveFailed(msg.requestId(), msg.equipmentId(), "equipment not found"));
                    continue;
                }
                Equipment e = eOpt.get();
                if (!"AVAILABLE".equalsIgnoreCase(e.getStatus())) {
                    template.send("inventory.reserve_failed", key, new Events.InventoryReserveFailed(msg.requestId(), msg.equipmentId(), "not available"));
                    continue;
                }
                e.setStatus("RESERVED");
                repo.save(e);
                template.send("inventory.reserved", key, new Events.InventoryReserved(msg.requestId(), msg.equipmentId()));
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
                repo.findById(msg.equipmentId()).ifPresentOrElse(e -> {
                    e.setStatus("AVAILABLE");
                    repo.save(e);
                    template.send("inventory.released", key, new Events.InventoryReleased(msg.requestId(), msg.equipmentId()));
                }, () -> template.send("inventory.released", key, new Events.InventoryReleased(msg.requestId(), msg.equipmentId())));
            } catch (Exception ex) {
                invalidRouter.routeInvalid("procurement.rejected.invalid", key, rec.value(), ex.getMessage());
            }
        }
    }
}
