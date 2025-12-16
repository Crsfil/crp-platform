package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentRepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EquipmentRepairOrderRepository extends JpaRepository<EquipmentRepairOrder, Long> {

    boolean existsByEquipmentIdAndStatusIn(Long equipmentId, Collection<String> statuses);

    List<EquipmentRepairOrder> findTop50ByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
}

