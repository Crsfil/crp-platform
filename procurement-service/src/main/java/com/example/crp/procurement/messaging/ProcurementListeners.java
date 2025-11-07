package com.example.crp.procurement.messaging;

import com.example.crp.procurement.domain.ProcurementRequest;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ProcurementListeners {
    private final ProcurementRequestRepository repo;
    public ProcurementListeners(ProcurementRequestRepository repo) { this.repo = repo; }

    @KafkaListener(topics = "inventory.reserved", groupId = "procurement")
    public void onReserved(@Payload Events.InventoryReserved msg) {
        repo.findById(msg.requestId()).ifPresent(pr -> { pr.setStatus("RESERVED"); repo.save(pr); });
    }

    @KafkaListener(topics = "inventory.reserve_failed", groupId = "procurement")
    public void onReserveFailed(@Payload Events.InventoryReserveFailed msg) {
        repo.findById(msg.requestId()).ifPresent(pr -> { pr.setStatus("FAILED"); repo.save(pr); });
    }

    @KafkaListener(topics = "inventory.released", groupId = "procurement")
    public void onReleased(@Payload Events.InventoryReleased msg) {
        repo.findById(msg.requestId()).ifPresent(pr -> { pr.setStatus("REJECTED"); repo.save(pr); });
    }
}

