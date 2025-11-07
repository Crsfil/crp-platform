package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.ProcurementRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementRequestRepository extends JpaRepository<ProcurementRequest, Long> {
}

