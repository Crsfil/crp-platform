package com.example.crp.procurement.web;

import com.example.crp.procurement.domain.Supplier;
import com.example.crp.procurement.service.SupplierService;
import com.example.crp.procurement.web.dto.ProcurementDtos;
import com.example.crp.procurement.web.dto.ProcurementMappers;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suppliers")
public class SuppliersController {
    private final SupplierService service;

    public SuppliersController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<ProcurementDtos.SupplierDto> list() {
        return service.list().stream().map(ProcurementMappers::toSupplier).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<ProcurementDtos.SupplierDto> create(@Valid @RequestBody ProcurementDtos.CreateSupplier req) {
        Supplier s = new Supplier();
        s.setName(req.name());
        s.setInn(req.inn());
        s.setKpp(req.kpp());
        s.setEmail(req.email());
        Supplier saved = service.create(s);
        return ResponseEntity.status(201).body(ProcurementMappers.toSupplier(saved));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProcurementDtos.SupplierDto> status(@PathVariable Long id, @RequestParam String status) {
        try {
            return ResponseEntity.ok(ProcurementMappers.toSupplier(service.setStatus(id, status)));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
