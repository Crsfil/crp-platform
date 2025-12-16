package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.repo.EquipmentRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {
    private final EquipmentRepository repository;

    public EquipmentController(EquipmentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<Equipment> list() { return repository.findAll(); }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public org.springframework.http.ResponseEntity<Equipment> create(@Valid @RequestBody Equipment e) {
        if (e.getStatus() == null) {
            e.setStatus("AVAILABLE");
        } else if (e.getStatus().trim().isEmpty()) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
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
                                                  @RequestParam("status") String status) {
        return repository.findById(id)
                .map(e -> { e.setStatus(status); return ResponseEntity.ok(repository.save(e)); })
                .orElse(ResponseEntity.notFound().build());
    }
}
