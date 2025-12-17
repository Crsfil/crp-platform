package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentValuation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentValuationRepository extends JpaRepository<EquipmentValuation, Long> {
    List<EquipmentValuation> findTop20ByEquipmentIdOrderByValuatedAtDesc(Long equipmentId);
}
