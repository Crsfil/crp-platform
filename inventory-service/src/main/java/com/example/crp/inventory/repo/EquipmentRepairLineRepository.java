package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.EquipmentRepairLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface EquipmentRepairLineRepository extends JpaRepository<EquipmentRepairLine, Long> {

    List<EquipmentRepairLine> findByRepairIdOrderByIdAsc(Long repairId);

    @Query("select coalesce(sum(l.totalCost), 0) from EquipmentRepairLine l where l.repairId = :repairId")
    BigDecimal sumTotalCostByRepairId(@Param("repairId") Long repairId);
}

