package com.example.crp.agreement.repo;

import com.example.crp.agreement.domain.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {}

