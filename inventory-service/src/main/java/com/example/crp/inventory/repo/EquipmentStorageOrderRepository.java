package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentStorageOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentStorageOrderRepository extends JpaRepository<EquipmentStorageOrder, Long> {
    List<EquipmentStorageOrder> findTop50ByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
    java.util.Optional<EquipmentStorageOrder> findFirstByProcurementServiceOrderId(Long procurementServiceOrderId);
    java.util.Optional<EquipmentStorageOrder> findFirstByEquipmentIdAndStatusOrderByCreatedAtDesc(Long equipmentId, String status);
}
