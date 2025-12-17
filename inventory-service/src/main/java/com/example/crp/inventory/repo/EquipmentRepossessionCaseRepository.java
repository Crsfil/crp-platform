package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentRepossessionCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepossessionCaseRepository extends JpaRepository<EquipmentRepossessionCase, Long> {
    List<EquipmentRepossessionCase> findTop20ByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
    Optional<EquipmentRepossessionCase> findFirstByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
    boolean existsByEquipmentIdAndStatusIn(Long equipmentId, List<String> statuses);
}
