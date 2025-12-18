package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.security.EquipmentAccessPolicy;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {
    private final EquipmentRepository repository;
    private final EquipmentAccessPolicy equipmentAccessPolicy;

    public EquipmentController(EquipmentRepository repository, EquipmentAccessPolicy equipmentAccessPolicy) {
        this.repository = repository;
        this.equipmentAccessPolicy = equipmentAccessPolicy;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<Equipment> list(org.springframework.security.core.Authentication auth) {
        return equipmentAccessPolicy.filterReadAllowed(auth, repository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public org.springframework.http.ResponseEntity<Equipment> create(@Valid @RequestBody Equipment e,
                                                                    org.springframework.security.core.Authentication auth) {
        if (e.getStatus() == null) {
            e.setStatus("AVAILABLE");
        } else if (e.getStatus().trim().isEmpty()) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
        equipmentAccessPolicy.assertWriteAllowed(auth, e.getLocationId());
        Equipment saved = repository.save(e);
        if (saved.getInventoryNumber() == null || saved.getInventoryNumber().isBlank()) {
            saved.setInventoryNumber("INV-" + saved.getId());
            saved = repository.save(saved);
        }
        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<Equipment> updateStatus(@PathVariable("id") Long id,
                                                  @RequestParam("status") String status,
                                                  org.springframework.security.core.Authentication auth) {
        return repository.findById(id)
                .map(e -> {
                    equipmentAccessPolicy.assertWriteAllowed(auth, e);
                    e.setStatus(status);
                    return ResponseEntity.ok(repository.save(e));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
