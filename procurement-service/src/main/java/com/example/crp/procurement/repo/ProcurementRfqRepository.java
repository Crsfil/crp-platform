package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.ProcurementRfq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementRfqRepository extends JpaRepository<ProcurementRfq, Long> {
}
