package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.EquipmentSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/equipment")
public class EquipmentSearchController {

    private final EquipmentRepository repository;

    public EquipmentSearchController(EquipmentRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<Page<Equipment>> search(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "locationId", required = false) Long locationId,
            @RequestParam(value = "responsible", required = false) String responsible,
            @RequestParam(value = "inventoryNumber", required = false) String inventoryNumber,
            @RequestParam(value = "serialNumber", required = false) String serialNumber,
            @RequestParam(value = "manufacturer", required = false) String manufacturer,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            Pageable pageable
    ) {
        Specification<Equipment> spec = Specification.where(EquipmentSpecifications.statusEquals(status))
                .and(EquipmentSpecifications.locationIdEquals(locationId))
                .and(EquipmentSpecifications.responsibleEquals(responsible))
                .and(EquipmentSpecifications.inventoryNumberEquals(inventoryNumber))
                .and(EquipmentSpecifications.serialNumberEquals(serialNumber))
                .and(EquipmentSpecifications.manufacturerLike(manufacturer))
                .and(EquipmentSpecifications.typeLike(type))
                .and(EquipmentSpecifications.priceBetween(minPrice, maxPrice));
        return ResponseEntity.ok(repository.findAll(spec, pageable));
    }
}

