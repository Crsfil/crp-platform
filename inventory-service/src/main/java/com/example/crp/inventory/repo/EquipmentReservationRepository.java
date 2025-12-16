package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipmentReservationRepository extends JpaRepository<EquipmentReservation, Long> {
    Optional<EquipmentReservation> findByRequestId(Long requestId);

    Optional<EquipmentReservation> findByRequestIdAndStatus(Long requestId, String status);
}

