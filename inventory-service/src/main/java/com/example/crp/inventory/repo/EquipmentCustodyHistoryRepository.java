package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentCustodyHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentCustodyHistoryRepository extends JpaRepository<EquipmentCustodyHistory, Long> {
    List<EquipmentCustodyHistory> findTop50ByEquipmentIdOrderByFromTsDesc(Long equipmentId);
}
