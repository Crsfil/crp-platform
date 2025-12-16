package com.example.crp.inventory.web;

import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.service.EquipmentLeaseService;
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
public class EquipmentLeaseController {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentLeaseService leaseService;

    public EquipmentLeaseController(EquipmentRepository equipmentRepository, EquipmentLeaseService leaseService) {
        this.equipmentRepository = equipmentRepository;
        this.leaseService = leaseService;
    }

    @PostMapping("/{id}/lease/start")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> start(@PathVariable("id") Long id,
                                   @Valid @RequestBody InventoryDtos.StartLease req,
                                   Authentication auth,
                                   HttpServletRequest servletRequest) {
        try {
            if (equipmentRepository.findById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            var lease = leaseService.start(id, req.customerLocationId(), req.agreementId(), req.customerId(),
                    req.expectedReturnAt(), req.note(), user, correlationId, auth);
            return ResponseEntity.ok(InventoryMappers.toLease(lease));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/lease/return")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> returnFromLease(@PathVariable("id") Long id,
                                             @Valid @RequestBody InventoryDtos.ReturnLease req,
                                             Authentication auth,
                                             HttpServletRequest servletRequest) {
        try {
            if (equipmentRepository.findById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            var lease = leaseService.returnFromLease(id, req.returnLocationId(), req.toStatus(), req.note(), user, correlationId, auth);
            return ResponseEntity.ok(InventoryMappers.toLease(lease));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/lease/repossess")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> repossess(@PathVariable("id") Long id,
                                       @Valid @RequestBody InventoryDtos.RepossessLease req,
                                       Authentication auth,
                                       HttpServletRequest servletRequest) {
        try {
            if (equipmentRepository.findById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            var lease = leaseService.repossess(id, req.repossessLocationId(), req.note(), user, correlationId, auth);
            return ResponseEntity.ok(InventoryMappers.toLease(lease));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/lease/active")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<?> active(@PathVariable("id") Long id) {
        try {
            if (equipmentRepository.findById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(InventoryMappers.toLease(leaseService.active(id)));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/leases")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.LeaseDto>> history(@PathVariable("id") Long id) {
        if (equipmentRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(leaseService.history(id).stream().map(InventoryMappers::toLease).toList());
    }
}

