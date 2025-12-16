package com.example.crp.inventory.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.service.EquipmentPassportService;

@RestController
@RequestMapping("/equipment")
public class EquipmentPassportController {

    private final EquipmentPassportService service;

    public EquipmentPassportController(EquipmentPassportService service) {
        this.service = service;
    }

    public record UpdatePassport(
            String inventoryNumber,
            String serialNumber,
            String manufacturer,
            String model,
            String type,
            String condition,
            java.math.BigDecimal price,
            Long locationId,
            String responsibleUsername
    ) {
        EquipmentPassportService.EquipmentPassportUpdate toUpdate() {
            return new EquipmentPassportService.EquipmentPassportUpdate(
                    inventoryNumber, serialNumber, manufacturer, model, type, condition, price, locationId, responsibleUsername
            );
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<Equipment> update(@PathVariable("id") Long id,
                                            @Valid @RequestBody UpdatePassport req) {
        try {
            return ResponseEntity.ok(service.update(id, req.toUpdate()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
