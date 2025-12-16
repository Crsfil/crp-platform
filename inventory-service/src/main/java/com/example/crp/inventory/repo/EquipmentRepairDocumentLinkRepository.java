package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentRepairDocumentLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepairDocumentLinkRepository extends JpaRepository<EquipmentRepairDocumentLink, Long> {

    List<EquipmentRepairDocumentLink> findByRepairIdOrderByCreatedAtDesc(Long repairId);
}

