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
    public List<Equipment> list() { return repository.findAll(); }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public Equipment create(@Valid @RequestBody Equipment e) { return repository.save(e); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<Equipment> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return repository.findById(id)
                .map(e -> { e.setStatus(status); return ResponseEntity.ok(repository.save(e)); })
                .orElse(ResponseEntity.notFound().build());
    }
}
