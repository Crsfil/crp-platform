package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.ProcurementRequestLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcurementRequestLineRepository extends JpaRepository<ProcurementRequestLine, Long> {
    List<ProcurementRequestLine> findByRequestIdOrderByIdAsc(Long requestId);
}

