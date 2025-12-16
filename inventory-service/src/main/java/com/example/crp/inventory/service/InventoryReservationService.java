package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentReservation;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.EquipmentReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class InventoryReservationService {

    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_RESERVED = "RESERVED";

    private static final String RESERVATION_ACTIVE = "ACTIVE";
    private static final String RESERVATION_RELEASED = "RELEASED";

    private final EquipmentRepository equipmentRepository;
    private final EquipmentReservationRepository reservationRepository;
    private final OutboxService outboxService;

    public InventoryReservationService(EquipmentRepository equipmentRepository,
                                       EquipmentReservationRepository reservationRepository,
                                       OutboxService outboxService) {
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public void reserveFromProcurementApproval(Events.ProcurementApproved msg) {
        if (msg == null || msg.requestId() == null || msg.equipmentId() == null) {
            return;
        }

        if (reservationRepository.findByRequestIdAndStatus(msg.requestId(), RESERVATION_ACTIVE).isPresent()) {
            return;
        }

        Equipment equipment = equipmentRepository.findById(msg.equipmentId()).orElse(null);
        if (equipment == null) {
            outboxService.enqueue("EquipmentReservation", msg.requestId(), "inventory.reserve_failed",
                    "InventoryReserveFailed", new Events.InventoryReserveFailed(msg.requestId(), msg.equipmentId(), "equipment not found"));
            return;
        }

        int updated = equipmentRepository.updateStatusIfEquals(msg.equipmentId(), STATUS_AVAILABLE, STATUS_RESERVED);
        if (updated == 0) {
            outboxService.enqueue("EquipmentReservation", msg.requestId(), "inventory.reserve_failed",
                    "InventoryReserveFailed", new Events.InventoryReserveFailed(msg.requestId(), msg.equipmentId(), "not available"));
            return;
        }

        EquipmentReservation reservation = new EquipmentReservation();
        reservation.setRequestId(msg.requestId());
        reservation.setEquipmentId(msg.equipmentId());
        reservation.setStatus(RESERVATION_ACTIVE);
        reservationRepository.save(reservation);

        outboxService.enqueue("EquipmentReservation", msg.requestId(), "inventory.reserved",
                "InventoryReserved", new Events.InventoryReserved(msg.requestId(), msg.equipmentId()));
    }

    @Transactional
    public void releaseFromProcurementReject(Events.ProcurementRejected msg) {
        if (msg == null || msg.requestId() == null || msg.equipmentId() == null) {
            return;
        }

        reservationRepository.findByRequestIdAndStatus(msg.requestId(), RESERVATION_ACTIVE).ifPresent(r -> {
            r.setStatus(RESERVATION_RELEASED);
            r.setReleasedAt(OffsetDateTime.now());
            reservationRepository.save(r);
        });

        equipmentRepository.updateStatusIfEquals(msg.equipmentId(), STATUS_RESERVED, STATUS_AVAILABLE);

        outboxService.enqueue("EquipmentReservation", msg.requestId(), "inventory.released",
                "InventoryReleased", new Events.InventoryReleased(msg.requestId(), msg.equipmentId()));
    }
}

