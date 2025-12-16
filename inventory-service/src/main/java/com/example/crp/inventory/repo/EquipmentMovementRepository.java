package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentMovementRepository extends JpaRepository<EquipmentMovement, Long> {
    List<EquipmentMovement> findTop200ByEquipmentIdOrderByMovedAtDesc(Long equipmentId);
}

