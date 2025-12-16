package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentMovement;
import com.example.crp.inventory.domain.EquipmentStatusHistory;
import com.example.crp.inventory.domain.EquipmentStatus;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentMovementRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.EquipmentStatusHistoryRepository;
import com.example.crp.inventory.repo.LocationRepository;
import com.example.crp.inventory.security.LocationAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentLifecycleService {

    private final EquipmentRepository equipmentRepository;
    private final LocationRepository locationRepository;
    private final EquipmentMovementRepository movementRepository;
    private final EquipmentStatusHistoryRepository statusHistoryRepository;
    private final OutboxService outboxService;
    private final LocationAccessPolicy locationAccessPolicy;

    public EquipmentLifecycleService(EquipmentRepository equipmentRepository,
                                    LocationRepository locationRepository,
                                    EquipmentMovementRepository movementRepository,
                                    EquipmentStatusHistoryRepository statusHistoryRepository,
                                    OutboxService outboxService,
                                    LocationAccessPolicy locationAccessPolicy) {
        this.equipmentRepository = equipmentRepository;
        this.locationRepository = locationRepository;
        this.movementRepository = movementRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.outboxService = outboxService;
        this.locationAccessPolicy = locationAccessPolicy;
    }

    @Transactional
    public Equipment transfer(Long equipmentId,
                              Long toLocationId,
                              String responsibleUsername,
                              String reason,
                              String movedBy,
                              String correlationId,
                              org.springframework.security.core.Authentication auth) {
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow();
        if (toLocationId == null) {
            throw new IllegalArgumentException("toLocationId is required");
        }
        var toLocation = locationRepository.findById(toLocationId).orElse(null);
        if (toLocation == null) {
            throw new IllegalArgumentException("target location not found");
        }
        if (toLocation.getStatus() != null && "INACTIVE".equalsIgnoreCase(toLocation.getStatus())) {
            throw new IllegalStateException("target location is INACTIVE");
        }

        Long fromLocationId = equipment.getLocationId();
        if (fromLocationId != null) {
            var fromLoc = locationRepository.findById(fromLocationId).orElse(null);
            if (fromLoc != null) {
                locationAccessPolicy.assertWriteAllowed(auth, fromLoc);
            }
        }
        locationAccessPolicy.assertWriteAllowed(auth, toLocation);

        equipment.setLocationId(toLocationId);
        if (responsibleUsername != null && !responsibleUsername.isBlank()) {
            equipment.setResponsibleUsername(responsibleUsername.trim());
        }
        Equipment saved = equipmentRepository.save(equipment);

        EquipmentMovement movement = new EquipmentMovement();
        movement.setEquipmentId(saved.getId());
        movement.setFromLocationId(fromLocationId);
        movement.setToLocationId(toLocationId);
        movement.setMovedBy(movedBy);
        movement.setReason(reason);
        movement.setCorrelationId(correlationId);
        movementRepository.save(movement);

        outboxService.enqueue("Equipment", saved.getId(), "inventory.equipment.transferred",
                "InventoryEquipmentTransferred", new Events.InventoryEquipmentTransferred(
                        saved.getId(), fromLocationId, toLocationId, saved.getResponsibleUsername(), movedBy, reason, correlationId
                ));
        return saved;
    }

    @Transactional
    public Equipment changeStatus(Long equipmentId,
                                 String toStatus,
                                 String reason,
                                 String changedBy,
                                 String correlationId,
                                 org.springframework.security.core.Authentication auth) {
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow();
        if (toStatus == null || toStatus.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        String normalized = toStatus.trim().toUpperCase();
        EquipmentStatus target = EquipmentStatus.parseOrNull(normalized);
        if (target == null) {
            throw new IllegalArgumentException("Unknown status: " + normalized);
        }
        String from = equipment.getStatus();
        if (from != null && from.trim().equalsIgnoreCase(normalized)) {
            return equipment;
        }

        EquipmentStatus current = EquipmentStatus.parseOrNull(from);
        if (current != null && !EquipmentStatus.allowedNext(current).contains(target)) {
            throw new IllegalStateException("Invalid status transition: " + current + " -> " + target);
        }

        Long locId = equipment.getLocationId();
        if (locId != null) {
            var loc = locationRepository.findById(locId).orElse(null);
            if (loc != null) {
                locationAccessPolicy.assertWriteAllowed(auth, loc);
            }
        }

        equipment.setStatus(normalized);
        Equipment saved = equipmentRepository.save(equipment);

        EquipmentStatusHistory h = new EquipmentStatusHistory();
        h.setEquipmentId(saved.getId());
        h.setFromStatus(from == null ? null : from.trim().toUpperCase());
        h.setToStatus(normalized);
        h.setChangedBy(changedBy);
        h.setReason(reason);
        h.setCorrelationId(correlationId);
        statusHistoryRepository.save(h);

        outboxService.enqueue("Equipment", saved.getId(), "inventory.equipment.status_changed",
                "InventoryEquipmentStatusChanged", new Events.InventoryEquipmentStatusChanged(
                        saved.getId(),
                        from == null ? null : from.trim().toUpperCase(),
                        normalized,
                        changedBy,
                        reason,
                        correlationId
                ));
        return saved;
    }
}
