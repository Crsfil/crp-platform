package com.example.crp.procurement.web;

import com.example.crp.procurement.domain.ProcurementRequest;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import com.example.crp.procurement.service.ProcurementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/requests")
public class ProcurementController {
    private final ProcurementRequestRepository repository;
    private final ProcurementService procurementService;
    public ProcurementController(ProcurementRequestRepository repository, ProcurementService procurementService) {
        this.repository = repository; this.procurementService = procurementService; }

    @GetMapping
    public List<ProcurementRequest> list() { return repository.findAll(); }

    @PostMapping
    public ProcurementRequest create(@Valid @RequestBody ProcurementRequest pr, Authentication auth) {
        return procurementService.create(pr);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PROCUREMENT_APPROVE') or hasRole('ADMIN')")
    public ResponseEntity<ProcurementRequest> approve(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(procurementService.approve(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PROCUREMENT_APPROVE') or hasRole('ADMIN')")
    public ResponseEntity<ProcurementRequest> reject(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(procurementService.reject(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
