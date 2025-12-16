package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.ProcurementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface ProcurementRequestRepository extends JpaRepository<ProcurementRequest, Long> {
    @EntityGraph(attributePaths = "lines")
    Optional<ProcurementRequest> findWithLinesById(Long id);
}
