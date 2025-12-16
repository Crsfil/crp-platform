package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentInspectionFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentInspectionFindingRepository extends JpaRepository<EquipmentInspectionFinding, Long> {

    List<EquipmentInspectionFinding> findByInspectionIdOrderByIdAsc(Long inspectionId);
}

