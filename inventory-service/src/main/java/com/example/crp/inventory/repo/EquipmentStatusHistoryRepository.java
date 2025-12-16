package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentStatusHistoryRepository extends JpaRepository<EquipmentStatusHistory, Long> {
    List<EquipmentStatusHistory> findTop200ByEquipmentIdOrderByChangedAtDesc(Long equipmentId);
}

