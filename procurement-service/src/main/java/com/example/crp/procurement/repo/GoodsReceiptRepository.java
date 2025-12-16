package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    @EntityGraph(attributePaths = "lines")
    Optional<GoodsReceipt> findWithLinesById(Long id);
}
