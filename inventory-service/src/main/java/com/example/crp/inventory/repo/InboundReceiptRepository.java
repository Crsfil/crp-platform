package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.InboundReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundReceiptRepository extends JpaRepository<InboundReceipt, Long> {
}

