package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {
    List<PurchaseOrderLine> findByPurchaseOrderIdOrderByIdAsc(Long purchaseOrderId);
}

