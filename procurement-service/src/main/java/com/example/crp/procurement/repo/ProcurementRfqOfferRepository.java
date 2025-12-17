package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.ProcurementRfqOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcurementRfqOfferRepository extends JpaRepository<ProcurementRfqOffer, Long> {
    List<ProcurementRfqOffer> findByRfqId(Long rfqId);
}
