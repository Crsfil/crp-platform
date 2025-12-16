package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EquipmentDocumentRepository extends JpaRepository<EquipmentDocument, UUID> {
    List<EquipmentDocument> findByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
}

