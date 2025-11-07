package com.example.crp.inventory.messaging;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class InventoryListeners {
    private final EquipmentRepository repo;
    private final KafkaTemplate<String, Object> template;
    public InventoryListeners(EquipmentRepository repo, KafkaTemplate<String, Object> template) {
        this.repo = repo; this.template = template;
    }

    @KafkaListener(topics = "procurement.approved", groupId = "inventory")
    public void onApproved(@Payload Events.ProcurementApproved msg) {
        Optional<Equipment> eOpt = repo.findById(msg.equipmentId());
        if (eOpt.isEmpty()) {
            template.send("inventory.reserve_failed", new Events.InventoryReserveFailed(msg.requestId(), msg.equipmentId(), "equipment not found"));
            return;
        }
        Equipment e = eOpt.get();
        if (!"AVAILABLE".equalsIgnoreCase(e.getStatus())) {
            template.send("inventory.reserve_failed", new Events.InventoryReserveFailed(msg.requestId(), msg.equipmentId(), "not available"));
            return;
        }
        e.setStatus("RESERVED");
        repo.save(e);
        template.send("inventory.reserved", new Events.InventoryReserved(msg.requestId(), msg.equipmentId()));
    }

    @KafkaListener(topics = "procurement.rejected", groupId = "inventory")
    public void onRejected(@Payload Events.ProcurementRejected msg) {
        repo.findById(msg.equipmentId()).ifPresent(e -> {
            e.setStatus("AVAILABLE");
            repo.save(e);
            template.send("inventory.released", new Events.InventoryReleased(msg.requestId(), msg.equipmentId()));
        });
    }
}

