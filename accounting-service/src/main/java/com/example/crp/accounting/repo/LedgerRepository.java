package com.example.crp.accounting.repo;

import com.example.crp.accounting.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {}

