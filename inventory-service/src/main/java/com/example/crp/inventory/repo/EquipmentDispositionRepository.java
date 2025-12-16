package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentDisposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EquipmentDispositionRepository extends JpaRepository<EquipmentDisposition, Long> {

    boolean existsByEquipmentIdAndStatusIn(Long equipmentId, Collection<String> statuses);

    List<EquipmentDisposition> findTop50ByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
}

