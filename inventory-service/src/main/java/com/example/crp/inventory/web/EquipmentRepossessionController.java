package com.example.crp.inventory.web;

import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.service.EquipmentRepossessionService;
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
public class EquipmentRepossessionController {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentRepossessionService service;
    private final com.example.crp.inventory.security.MfaPolicy mfaPolicy;

    public EquipmentRepossessionController(EquipmentRepository equipmentRepository,
                                           EquipmentRepossessionService service,
                                           com.example.crp.inventory.security.MfaPolicy mfaPolicy) {
        this.equipmentRepository = equipmentRepository;
        this.service = service;
        this.mfaPolicy = mfaPolicy;
    }

    @PostMapping("/{id}/repossessions")
    @PreAuthorize("hasAuthority('INVENTORY_REPOSSESS') or hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> openCase(@PathVariable("id") Long equipmentId,
                                      @Valid @RequestBody InventoryDtos.CreateRepossessionCase req,
                                      Authentication auth,
                                      HttpServletRequest servletRequest) {
        try {
            if (equipmentRepository.findById(equipmentId).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            if (!mfaPolicy.isSatisfied(auth)) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "mfa_required"));
            }
            var saved = service.openCase(equipmentId, req.triggerReason(), req.decisionRef(), req.targetLocationId(), user, correlationId, auth);
            return ResponseEntity.status(201).body(InventoryMappers.toRepossessionCase(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/repossessions")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.RepossessionCaseDto>> listCases(@PathVariable("id") Long equipmentId) {
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.listCases(equipmentId).stream().map(InventoryMappers::toRepossessionCase).toList());
    }

    @PostMapping("/{id}/storage-orders")
    @PreAuthorize("hasAuthority('INVENTORY_STORAGE') or hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> intakeToStorage(@PathVariable("id") Long equipmentId,
                                             @Valid @RequestBody InventoryDtos.CreateStorageOrder req,
                                             Authentication auth,
                                             HttpServletRequest servletRequest) {
        try {
            if (equipmentRepository.findById(equipmentId).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            if (!mfaPolicy.isSatisfied(auth)) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "mfa_required"));
            }
            var saved = service.intakeToStorage(equipmentId,
                    req.storageLocationId(),
                    req.vendorName(),
                    req.vendorInn(),
                    req.slaUntil(),
                    req.expectedCost(),
                    req.currency(),
                    req.procurementServiceOrderId(),
                    req.note(),
                    user,
                    correlationId,
                    auth);
            return ResponseEntity.status(201).body(InventoryMappers.toStorageOrder(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/storage-orders")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.StorageOrderDto>> storageOrders(@PathVariable("id") Long equipmentId) {
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.listStorageOrders(equipmentId).stream().map(InventoryMappers::toStorageOrder).toList());
    }

    @PostMapping("/{id}/valuations")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasAuthority('INVENTORY_REPOSSESS') or hasRole('ADMIN')")
    public ResponseEntity<?> valuation(@PathVariable("id") Long equipmentId,
                                       @Valid @RequestBody InventoryDtos.CreateValuation req,
                                       Authentication auth,
                                       HttpServletRequest servletRequest) {
        try {
            if (equipmentRepository.findById(equipmentId).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String user = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            if (!mfaPolicy.isSatisfied(auth)) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "mfa_required"));
            }
            var saved = service.recordValuation(equipmentId,
                    req.valuationAmount(),
                    req.liquidationAmount(),
                    req.currency(),
                    req.valuatedAt(),
                    req.vendorName(),
                    req.vendorInn(),
                    req.note(),
                    user,
                    correlationId,
                    auth);
            return ResponseEntity.status(201).body(InventoryMappers.toValuation(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/valuations")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.ValuationDto>> valuations(@PathVariable("id") Long equipmentId) {
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.listValuations(equipmentId).stream().map(InventoryMappers::toValuation).toList());
    }

    @GetMapping("/{id}/custody")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.CustodyDto>> custody(@PathVariable("id") Long equipmentId) {
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.listCustody(equipmentId).stream().map(InventoryMappers::toCustody).toList());
    }

}
