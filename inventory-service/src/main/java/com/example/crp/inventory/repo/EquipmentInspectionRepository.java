package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EquipmentInspectionRepository extends JpaRepository<EquipmentInspection, Long> {

    boolean existsByEquipmentIdAndStatusIn(Long equipmentId, Collection<String> statuses);

    List<EquipmentInspection> findTop50ByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
}

