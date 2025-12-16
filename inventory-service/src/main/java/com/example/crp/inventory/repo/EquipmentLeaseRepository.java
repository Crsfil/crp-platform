package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentLease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentLeaseRepository extends JpaRepository<EquipmentLease, Long> {

    boolean existsByEquipmentIdAndStatus(Long equipmentId, String status);

    Optional<EquipmentLease> findFirstByEquipmentIdAndStatusOrderByCreatedAtDesc(Long equipmentId, String status);

    List<EquipmentLease> findTop50ByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
}

