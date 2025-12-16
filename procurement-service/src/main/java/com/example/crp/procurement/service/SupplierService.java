package com.example.crp.procurement.service;

import com.example.crp.procurement.domain.Supplier;
import com.example.crp.procurement.repo.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {
    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    public List<Supplier> list() {
        return repository.findAll();
    }

    @Transactional
    public Supplier create(Supplier supplier) {
        supplier.setStatus("ACTIVE");
        return repository.save(supplier);
    }

    @Transactional
    public Supplier setStatus(Long id, String status) {
        Supplier s = repository.findById(id).orElseThrow();
        s.setStatus(status);
        return repository.save(s);
    }
}

