package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentDispositionDocumentLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentDispositionDocumentLinkRepository extends JpaRepository<EquipmentDispositionDocumentLink, Long> {

    List<EquipmentDispositionDocumentLink> findByDispositionIdOrderByCreatedAtDesc(Long dispositionId);
}

