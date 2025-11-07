package com.example.crp.billing.repo;

import com.example.crp.billing.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByAgreementId(Long agreementId);
}

