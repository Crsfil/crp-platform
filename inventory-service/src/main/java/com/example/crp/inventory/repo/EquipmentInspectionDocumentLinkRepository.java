package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentInspectionDocumentLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentInspectionDocumentLinkRepository extends JpaRepository<EquipmentInspectionDocumentLink, Long> {

    List<EquipmentInspectionDocumentLink> findByInspectionIdOrderByCreatedAtDesc(Long inspectionId);
}

