package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.ProcurementServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcurementServiceOrderRepository extends JpaRepository<ProcurementServiceOrder, Long> {
    List<ProcurementServiceOrder> findTop50ByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
    long countByServiceTypeAndStatusNotInAndSlaUntilBefore(String serviceType, List<String> statuses, java.time.OffsetDateTime before);
}
