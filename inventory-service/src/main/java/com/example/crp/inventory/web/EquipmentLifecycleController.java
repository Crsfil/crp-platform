package com.example.crp.inventory.web;

import com.example.crp.inventory.repo.EquipmentMovementRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.EquipmentStatusHistoryRepository;
import com.example.crp.inventory.service.EquipmentLifecycleService;
import com.example.crp.inventory.web.dto.InventoryDtos;
import com.example.crp.inventory.web.dto.InventoryMappers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipment")
public class EquipmentLifecycleController {

    private final EquipmentLifecycleService lifecycleService;
    private final EquipmentMovementRepository movementRepository;
    private final EquipmentStatusHistoryRepository statusHistoryRepository;
    private final EquipmentRepository equipmentRepository;

    public EquipmentLifecycleController(EquipmentLifecycleService lifecycleService,
                                        EquipmentMovementRepository movementRepository,
                                        EquipmentStatusHistoryRepository statusHistoryRepository,
                                        EquipmentRepository equipmentRepository) {
        this.lifecycleService = lifecycleService;
        this.movementRepository = movementRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> transfer(@PathVariable("id") Long id,
                                      @Valid @RequestBody InventoryDtos.TransferEquipment req,
                                      Authentication auth,
                                      HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(lifecycleService.transfer(id, req.toLocationId(), req.responsibleUsername(), req.reason(), user, correlationId, auth));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> changeStatus(@PathVariable("id") Long id,
                                          @Valid @RequestBody InventoryDtos.ChangeStatus req,
                                          Authentication auth,
                                          HttpServletRequest servletRequest) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(lifecycleService.changeStatus(id, req.status(), req.reason(), user, correlationId, auth));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/movements")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.MovementDto>> movements(@PathVariable("id") Long id) {
        if (equipmentRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(movementRepository.findTop200ByEquipmentIdOrderByMovedAtDesc(id).stream().map(InventoryMappers::toMovement).toList());
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.StatusHistoryDto>> statusHistory(@PathVariable("id") Long id) {
        if (equipmentRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(statusHistoryRepository.findTop200ByEquipmentIdOrderByChangedAtDesc(id).stream().map(InventoryMappers::toStatusHistory).toList());
    }
}
