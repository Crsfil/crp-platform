package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentPassportService {

    private final EquipmentRepository repository;
    private final OutboxService outboxService;

    public EquipmentPassportService(EquipmentRepository repository, OutboxService outboxService) {
        this.repository = repository;
        this.outboxService = outboxService;
    }

    @Transactional
    public Equipment update(Long equipmentId, EquipmentPassportUpdate update) {
        Equipment e = repository.findById(equipmentId).orElseThrow();
        if (update.inventoryNumber() != null) e.setInventoryNumber(trim(update.inventoryNumber(), 64));
        if (update.serialNumber() != null) e.setSerialNumber(trim(update.serialNumber(), 128));
        if (update.manufacturer() != null) e.setManufacturer(trim(update.manufacturer(), 128));
        if (update.model() != null) e.setModel(trim(update.model(), 128));
        if (update.type() != null) e.setType(trim(update.type(), 128));
        if (update.condition() != null) e.setCondition(trim(update.condition(), 32));
        if (update.price() != null) e.setPrice(update.price());
        if (update.locationId() != null) e.setLocationId(update.locationId());
        if (update.responsibleUsername() != null) e.setResponsibleUsername(trim(update.responsibleUsername(), 128));

        Equipment saved = repository.save(e);

        outboxService.enqueue("Equipment", saved.getId(), "inventory.equipment.passport_updated",
                "InventoryEquipmentPassportUpdated", new Events.InventoryEquipmentPassportUpdated(
                        saved.getId(),
                        saved.getInventoryNumber(),
                        saved.getSerialNumber(),
                        saved.getManufacturer(),
                        saved.getModel(),
                        saved.getType(),
                        saved.getCondition(),
                        saved.getLocationId(),
                        saved.getResponsibleUsername()
                ));
        return saved;
    }

    public record EquipmentPassportUpdate(
            String inventoryNumber,
            String serialNumber,
            String manufacturer,
            String model,
            String type,
            String condition,
            java.math.BigDecimal price,
            Long locationId,
            String responsibleUsername
    ) {}

    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() > max) {
            return t.substring(0, max);
        }
        return t;
    }
}

