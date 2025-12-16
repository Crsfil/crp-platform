package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}

